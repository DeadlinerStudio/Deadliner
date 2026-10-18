import Foundation
import CoreText
import CoreGraphics
let root = URL(fileURLWithPath: CommandLine.arguments[1])
let config = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("scripts/interface_icon_map.json"))) as! [String:Any]
let rows = config["icons"] as! [String:Any]
let url = root.appendingPathComponent("app/src/main/res/font/material_symbols_rounded_variable.ttf")
let descriptors = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as! [CTFontDescriptor]
let output = root.appendingPathComponent("third_party/interface-icons/material-reference")
try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
func number(_ n: CGFloat) -> String { String(format:"%.4f",Double(n)) }
var failed = [String]()
for key in rows.keys.sorted() {
    let name = key.components(separatedBy:"#")[0]
    let variation: [NSNumber:NSNumber] = [2003265652:330,1179208780:key.contains("#filled") ? 1:0,1196572996:0,1869640570:24]
    let descriptor = CTFontDescriptorCreateCopyWithAttributes(descriptors[0], [kCTFontVariationAttribute:variation] as CFDictionary)
    let font = CTFontCreateWithFontDescriptor(descriptor,24,nil)
    let line = CTLineCreateWithAttributedString(NSAttributedString(string:name,attributes:[NSAttributedString.Key(kCTFontAttributeName as String):font]))
    let runs = CTLineGetGlyphRuns(line) as! [CTRun]
    let count = runs.reduce(0) { $0 + CTRunGetGlyphCount($1) }
    if count != 1 {failed.append(name);continue}
    var glyph: CGGlyph = 0
    CTRunGetGlyphs(runs[0],CFRange(location:0,length:1),&glyph)
    guard let path = CTFontCreatePathForGlyph(font,glyph,nil) else {failed.append(name);continue}
    var d=""
    path.applyWithBlock { p in
        let e=p.pointee
        switch e.type {
        case .moveToPoint: d += "M\(number(e.points[0].x)) \(number(e.points[0].y)) "
        case .addLineToPoint: d += "L\(number(e.points[0].x)) \(number(e.points[0].y)) "
        case .addQuadCurveToPoint: d += "Q\(number(e.points[0].x)) \(number(e.points[0].y)) \(number(e.points[1].x)) \(number(e.points[1].y)) "
        case .addCurveToPoint: d += "C\(number(e.points[0].x)) \(number(e.points[0].y)) \(number(e.points[1].x)) \(number(e.points[1].y)) \(number(e.points[2].x)) \(number(e.points[2].y)) "
        case .closeSubpath: d += "Z "
        @unknown default: break
        }
    }
    let width=CTLineGetTypographicBounds(line,nil,nil,nil)
    let baseline=12+(CTFontGetAscent(font)-CTFontGetDescent(font))/2
    let svg="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><g transform=\"translate(\(12-width/2) \(baseline)) scale(1 -1)\"><path d=\"\(d)\" fill=\"black\"/></g></svg>"
    try svg.write(to: output.appendingPathComponent(key.replacingOccurrences(of:"#",with:"_")+".svg"),atomically:true,encoding:.utf8)
}
print("Rendered Material Symbols 330. Unsupported ligatures:",failed)
