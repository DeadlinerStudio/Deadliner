"""Bundle this local design prototype into one HTML file; no build dependencies."""
from pathlib import Path
from urllib.parse import quote
import re

root = Path(__file__).resolve().parent
html = (root / 'index.html').read_text().replace('href="./"', 'href="lifi-ai.html"')
logo = quote((root / 'lifi.svg').read_text(), safe='')
for stylesheet in ('styles.css', 'conversation.css', 'adaptive.css'):
    css = (root / stylesheet).read_text().replace('url(lifi.svg)', f'url("data:image/svg+xml,{logo}")')
    html = html.replace(f'<link rel="stylesheet" href="{stylesheet}">', '<style>\n' + css + '</style>')
script = (root / 'app.js').read_text()
html = html.replace('<script src="app.js"></script>', '<script>\n' + script + '\n</script>')
# Keep the exported artifact compact; the modules are the reviewable sources.
html = re.sub(r'\n\s*', '\n', html)
(root / 'lifi-ai.html').write_text(html)
print('Exported lifi-ai.html')
