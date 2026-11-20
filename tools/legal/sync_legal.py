#!/usr/bin/env python3
"""把服務條款、隱私權政策網站的內容轉成 App 內建的 JSON（app/src/main/assets/legal/）。

網站是唯一的原稿：改條款時先改網站 repo，再跑這支程式同步進 App。
App 不連網，所以條款要內建一份，玩家離線也看得到。

用法：python3 tools/legal/sync_legal.py <WrongButton-Terms 資料夾> <WrongButton-Privacy 資料夾>
"""
import html, json, re, sys
from html.parser import HTMLParser
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / 'app/src/main/assets/legal'
SITES = {'terms': 'https://wrongbutton-terms.pages.dev/', 'privacy': 'https://wrongbutton-privacy.pages.dev/'}


class Sections(HTMLParser):
    """只讀 <section class="level">：每一節的標題、任務一句話，以及段落、清單、小標、表格。"""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.sections, self.cur = [], None
        self.depth = 0          # 目前在 section 裡的巢狀深度
        self.buf = None         # 正在收集的行內文字（簡化過的 HTML）
        self.kind = None        # 正在收集的區塊種類
        self.lists = []         # 巢狀清單：'ul' / 'ol' 與計數
        self.table = None       # {'head': [...], 'rows': [...], 'span': {col: [剩幾列, 文字]}}
        self.cell = None
        self.skip = 0

    # ---- 工具 ----
    def start_block(self, kind):
        self.kind, self.buf = kind, []

    def end_block(self):
        if self.buf is None:
            return
        text = re.sub(r'\s+', ' ', ''.join(self.buf)).strip()
        kind, self.buf, self.kind = self.kind, None, None
        if not text:
            return
        if kind == 'h2':
            self.cur['title'] = text
        elif kind == 'badge':
            pass
        elif kind == 'mission':
            self.cur['mission'] = text
        else:
            self.cur['blocks'].append({'type': kind, 'text': text})

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        cls = a.get('class', '')
        if tag == 'section' and cls == 'level':
            self.cur = {'id': a['id'], 'title': '', 'mission': '', 'blocks': []}
            self.depth = 1
            return
        if self.cur is None:
            return
        if tag in ('section', 'div', 'ul', 'ol', 'table', 'header'):
            self.depth += 1
        if tag == 'span' and 'level-badge' in cls:
            self.start_block('badge')
        elif tag == 'h2':
            self.start_block('h2')
        elif tag == 'h3':
            self.start_block('h3')
        elif tag == 'p':
            self.start_block('mission' if cls == 'mission' else ('note' if 'muted' in cls else 'p'))
        elif tag in ('ul', 'ol'):
            self.lists.append([tag, 0])
        elif tag == 'li':
            lst = self.lists[-1]
            lst[1] += 1
            self.start_block('li')
            if lst[0] == 'ol':
                self.buf.append(f'{lst[1]}. ')
        elif tag == 'table':
            self.table = {'head': [], 'rows': [], 'span': {}}
        elif tag == 'tr' and self.table is not None:
            self.table['row'] = []
        elif tag in ('th', 'td') and self.table is not None:
            self.cell = {'tag': tag, 'rowspan': int(a.get('rowspan', '1'))}
            self.start_block('cell')
        elif tag == 'a' and self.buf is not None:
            self.buf.append(f'<a href="{html.escape(a.get("href", ""), quote=True)}">')
        elif tag == 'a' and 'chip' in cls:
            # 信箱膠囊：自己當一行連結
            self.start_block('link')
            self.buf.append(f'<a href="{html.escape(a.get("href", ""), quote=True)}">')
        elif tag in ('strong', 'b') and self.buf is not None:
            self.buf.append('<b>')
        elif tag == 'br' and self.buf is not None:
            self.buf.append(' ')

    def handle_endtag(self, tag):
        if self.cur is None:
            return
        if tag in ('h2', 'h3', 'p', 'li') or (tag == 'span' and self.kind == 'badge'):
            self.end_block()
        elif tag == 'a' and self.buf is not None:
            self.buf.append('</a>')
            if self.kind == 'link':
                self.end_block()
        elif tag in ('strong', 'b') and self.buf is not None:
            self.buf.append('</b>')
        elif tag in ('th', 'td') and self.cell is not None:
            text = re.sub(r'\s+', ' ', ''.join(self.buf)).strip()
            self.buf, self.kind = None, None
            self.cell['text'] = text
            self.table['row'].append(self.cell)
            self.cell = None
        elif tag == 'tr' and self.table is not None:
            row = self.table.pop('row')
            if row and all(c['tag'] == 'th' for c in row):
                self.table['head'] = [c['text'] for c in row]
            else:
                cells, it, col = [], iter(row), 0
                while col < len(self.table['head']):
                    span = self.table['span'].get(col)
                    if span and span[0] > 0:
                        cells.append(span[1]); span[0] -= 1
                    else:
                        c = next(it)
                        cells.append(c['text'])
                        if c['rowspan'] > 1:
                            self.table['span'][col] = [c['rowspan'] - 1, c['text']]
                    col += 1
                self.table['rows'].append(cells)
        if tag in ('ul', 'ol'):
            self.lists.pop()
        if tag == 'table' and self.table is not None:
            head = self.table['head']
            for cells in self.table['rows']:
                self.cur['blocks'].append({'type': 'row', 'cells': [[h, c] for h, c in zip(head, cells)]})
            self.table = None
        if tag in ('section', 'div', 'ul', 'ol', 'table', 'header'):
            self.depth -= 1
            if tag == 'section' and self.depth == 0:
                self.sections.append(self.cur)
                self.cur = None

    def handle_data(self, data):
        if self.buf is not None:
            self.buf.append(html.escape(data, quote=False))


def convert(name, folder):
    src = (Path(folder) / 'index.html').read_text(encoding='utf-8')
    p = Sections()
    p.feed(src)
    title = re.search(r'<h1 id="title">(.*?)</h1>', src).group(1)
    updated = re.search(r'最後更新：([^<]*)', src).group(1).strip()
    assert p.sections and all(s['title'] and s['blocks'] for s in p.sections), name
    doc = {'title': title, 'updated': updated, 'url': SITES[name], 'sections': p.sections}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f'{name}.json').write_text(json.dumps(doc, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(name, len(p.sections), 'sections,', sum(len(s['blocks']) for s in p.sections), 'blocks')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    convert('terms', sys.argv[1])
    convert('privacy', sys.argv[2])
