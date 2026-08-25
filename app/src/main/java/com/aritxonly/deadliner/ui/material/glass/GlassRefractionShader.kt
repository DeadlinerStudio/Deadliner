package com.aritxonly.deadliner.ui.material.glass

// The circular lens profile and rounded-rectangle SDF approach are informed by
// Kyant0/AndroidLiquidGlass (Apache-2.0): https://github.com/Kyant0/AndroidLiquidGlass
internal const val GlassRefractionShaderKey = "deadliner_glass_capsule_lens_v3"

internal const val GlassRefractionShader = """
    uniform shader source;
    uniform float2 content_origin;
    uniform float2 content_size;
    uniform float corner_radius;
    uniform float refraction_height;
    uniform float refraction_amount;
    uniform float depth_effect;
    uniform float chromatic_aberration;
    uniform float noise_coefficient;
    uniform float highlight_alpha;
    uniform float highlight_gray;

    float roundedRectSdf(float2 coord, float2 halfSize, float radius) {
        float2 corner = abs(coord) - (halfSize - float2(radius));
        float outside = length(max(corner, float2(0.0))) - radius;
        float inside = min(max(corner.x, corner.y), 0.0);
        return outside + inside;
    }

    float2 roundedRectGradient(float2 coord, float2 halfSize, float radius) {
        float2 corner = abs(coord) - (halfSize - float2(radius));
        if (corner.x >= 0.0 || corner.y >= 0.0) {
            return sign(coord) * normalize(max(corner, float2(0.0)) + float2(0.0001));
        }
        float horizontal = step(corner.y, corner.x);
        return sign(coord) * float2(horizontal, 1.0 - horizontal);
    }

    float circularLens(float x) {
        x = clamp(x, 0.0, 1.0);
        return 1.0 - sqrt(max(1.0 - x * x, 0.0));
    }

    float random(float2 coord, float seed) {
        return fract(sin(dot(coord, float2(6.9898 + seed, 78.233))) * (43734.5453 + seed));
    }

    half4 main(float2 position) {
        float2 halfSize = content_size * 0.5;
        float2 center = content_origin + halfSize;
        float2 centered = position - center;
        float radius = clamp(corner_radius, 0.0, min(halfSize.x, halfSize.y));
        float signedDistance = roundedRectSdf(centered, halfSize, radius);
        float innerDepth = max(-signedDistance, 0.0);
        float lensProgress = 1.0 - innerDepth / max(refraction_height, 0.001);
        float lens = circularLens(lensProgress);

        float gradientRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
        float2 normal = roundedRectGradient(centered, halfSize, gradientRadius);
        float2 radial = centered / max(length(centered), 0.001);
        normal = normalize(normal + radial * depth_effect + float2(0.0001));

        float2 sampleMin = content_origin + float2(0.5);
        float2 sampleMax = content_origin + content_size - float2(0.5);
        float2 refractedPosition = clamp(
            position - normal * refraction_amount * lens,
            sampleMin,
            sampleMax
        );
        float2 dispersion = normal * chromatic_aberration * lens;
        half4 color = source.eval(refractedPosition);
        if (chromatic_aberration > 0.001) {
            half4 redSample = source.eval(clamp(refractedPosition - dispersion, sampleMin, sampleMax));
            half4 blueSample = source.eval(clamp(refractedPosition + dispersion, sampleMin, sampleMax));
            color.r = redSample.r;
            color.b = blueSample.b;
        }

        float edgeWidth = max(1.0, refraction_height * 0.12);
        float rim = 1.0 - smoothstep(0.0, edgeWidth, innerDepth);
        float2 lightDirection = normalize(float2(-0.58, -0.82));
        float directional = pow(abs(dot(normal, lightDirection)), 4.0);
        float edgeLight = highlight_alpha * rim * (0.30 + directional * 0.70);
        color.rgb = mix(color.rgb, half3(highlight_gray), half(edgeLight));

        if (noise_coefficient > 0.0) {
            color.r += half((random(position, 0.0) - 0.5) * noise_coefficient);
            color.g += half((random(position, 1.0) - 0.5) * noise_coefficient);
            color.b += half((random(position, 2.0) - 0.5) * noise_coefficient);
        }
        return color;
    }
"""
