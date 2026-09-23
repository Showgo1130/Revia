# screenshot.png の作り方

`screenshot.png` は **[index.html](index.html) から起こした生成物**。手で描き直さない。デザインを変えたら、この手順で撮り直す。

撮っているのは 3 画面（教材一覧・教材詳細・復習一覧）。**`index.html` の実際のマークアップと CSS をそのまま抜いて**並べるので、見た目がずれない。

## 1. 並べる頁を作る

`index.html` から `<style>`・アイコンのスプライト・`.phone` の 3 つを抜き出して、横に並べた頁を作る。

```python
import io, re

s = io.open('docs/ui/index.html', encoding='utf-8', newline='').read().replace('\r\n', '\n')
style  = re.search(r'<style>.*?</style>', s, re.S).group(0)
sprite = re.search(r'<svg width="0" height="0".*?</svg>', s, re.S).group(0)
link   = re.search(r'<link rel="stylesheet" href="https://fonts\.googleapis[^>]*>', s).group(0)

# .phone を、入れ子の <div> を数えて切り出す
starts = [m.start() for m in re.finditer(r'<div class="phone"', s)]
def block(i):
    depth, tok = 0, re.compile(r'<div\b|</div>')
    for m in tok.finditer(s, starts[i]):
        depth += 1 if m.group(0) != '</div>' else -1
        if depth == 0:
            return s[starts[i]:m.end()]

# 頁に出てくる順の番号。0=画面1, 1=画面2, ... 10=画面11, 11=画面12, 12=画面14, 13=画面13
want = [(1, '教材一覧'), (7, '教材詳細'), (11, '復習一覧')]
cards = ''.join('<figure><figcaption>%s</figcaption>%s</figure>' % (n, block(i)) for i, n in want)

io.open('shot.html', 'w', encoding='utf-8', newline='\n').write('''<!doctype html><html lang="ja"><head><meta charset="utf-8">
%s
%s
<style>
  html,body{margin:0;background:#DCDDE1;font-family:"Noto Sans JP",sans-serif;}
  .sheet{display:flex;gap:36px;padding:44px 40px 36px;justify-content:center;align-items:flex-start;width:max-content;margin:0 auto;}
  figure{margin:0;display:block;width:360px;flex:none;}
  figcaption{margin-top:16px;text-align:center;font-size:14px;font-weight:700;color:#15171B;letter-spacing:.02em;}
  /* flex の中では .phone が width:100% で潰れるので、ここで固定する */
  .phone{width:360px !important;max-width:360px !important;height:720px !important;aspect-ratio:auto !important;margin:0 !important;flex:none !important;}
</style></head><body>
%s
<div class="sheet">%s</div>
</body></html>''' % (link, style, sprite, cards))
```

## 2. 撮る

ヘッドレスの Chrome で撮る。**`--virtual-time-budget` を入れないと、Lucide の CDN と Google Fonts が間に合わない。**

```bash
# Chrome の実行ファイルを指す。Windows なら Program Files の下、
# macOS なら /Applications の下。手元のパスをこのファイルに書き残さない
CHROME="<Chrome の実行ファイル>"

# Chrome が読める絶対パス。Git Bash の /c/... は読まれない
ROOT=$(cygpath -m "$(pwd)")   # macOS・Linux なら ROOT=$(pwd)

"$CHROME" \
  --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=2 \
  --window-size=1240,840 \
  --virtual-time-budget=8000 \
  --screenshot="$ROOT/docs/ui/screenshot.png" \
  "file:///$ROOT/shot.html"
```

**`--screenshot` も開く頁も絶対パスにする。** Chrome は相対パスを**自分の作業ディレクトリ**から解決するので、シェル側の位置とは合わず、`Failed to write file` で落ちる。`$(pwd)` から組み立てれば、手元のパスをこのファイルに書き残さずに済む。

`--force-device-scale-factor=2` で 2480 × 1680 になる。README に載せる大きさとして十分。

**枠は 360 × 720px にする。** `index.html` の `.phone` と同じ寸法で、**1px = 1dp** になる。ここを縮めると中身だけが大きく見え、寸法を見て設計を決めたときにずれる。

## 3. 確かめる

- **アイコンが本物の Lucide になっているか。** 歯車・`>`・`+`・縦三点が出ていれば読み込めている。のっぺりした代替が出ていたら `--virtual-time-budget` を伸ばす
- **ナビが枠の幅の 70% か**（決定 43）。枠は 360px なので 252px になる
- **ナビの選択が半分の幅いっぱいのカプセルか**（決定 27）
- **印が ○ ✓ ● か**、朱が「要復習」の数字と ● だけに出ているか（決定 24・25）
- 書体が Noto Sans JP になっているか。出ていなければ Google Fonts が間に合っていない
