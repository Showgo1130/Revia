/**
 * docs/design.md のワイヤーフレームを Figma に生成する（Issue #9）。
 *
 * 実行すると新しいページを作り、そこに 4 画面を横並びで置く。既存のページは触らない。
 *
 * **値の正本は docs/。** 状態の印・色の使い方・ナビの構成などは docs/decisions.md で
 * 決まっていて、ここはその実装。見た目を良くするためでも勝手に変えない。
 */

// ── Material 3 ベースライン（ライト）──────────────────────────────
// docs/ には無く、この文書で初めて決める値。実装は Compose の Material 3 を使うので
// ロール名を揃えておくと、そのまま持っていける。
const M3 = {
  primary: '#6750A4',
  onPrimary: '#FFFFFF',
  primaryContainer: '#EADDFF',
  onPrimaryContainer: '#21005D',
  surface: '#FEF7FF',
  surfaceContainer: '#F3EDF7',
  onSurface: '#1D1B20',
  onSurfaceVariant: '#49454F',
  outlineVariant: '#CAC4D0',
  tertiary: '#7D5260',
} as const;

/** M3 の type scale のうち、この 4 画面で使うものだけ。 */
const TYPE = {
  headlineSmall: { size: 24, line: 32, bold: false },
  titleMedium: { size: 16, line: 24, bold: true },
  bodyLarge: { size: 16, line: 24, bold: false },
  bodyMedium: { size: 14, line: 20, bold: false },
  labelLarge: { size: 14, line: 20, bold: true },
  labelMedium: { size: 12, line: 16, bold: true },
} as const;

const SCREEN = { width: 360, height: 800 };
const GAP = 80;
const PAD = 16;
/** ツリーの 1 段あたりの字下げ。**4 段目以降は増やさない**（decisions 10）。 */
const INDENT = 16;
const MAX_INDENT_LEVEL = 3;

// ── フォント ────────────────────────────────────────────────
// 日本語と絵文字が出る組み合わせを順に試す。1 つも無い環境でも落とさない。
type Fonts = { regular: FontName; medium: FontName };

const FONT_CANDIDATES: Fonts[] = [
  { regular: { family: 'Noto Sans JP', style: 'Regular' }, medium: { family: 'Noto Sans JP', style: 'Medium' } },
  { regular: { family: 'Noto Sans CJK JP', style: 'Regular' }, medium: { family: 'Noto Sans CJK JP', style: 'Medium' } },
  { regular: { family: 'Yu Gothic', style: 'Regular' }, medium: { family: 'Yu Gothic', style: 'Bold' } },
  { regular: { family: 'Hiragino Sans', style: 'W3' }, medium: { family: 'Hiragino Sans', style: 'W6' } },
  { regular: { family: 'Inter', style: 'Regular' }, medium: { family: 'Inter', style: 'Medium' } },
];

let fonts: Fonts;

async function loadFonts(): Promise<Fonts> {
  for (const candidate of FONT_CANDIDATES) {
    try {
      await figma.loadFontAsync(candidate.regular);
      await figma.loadFontAsync(candidate.medium);
      return candidate;
    } catch (_) {
      // この組み合わせは無い。次を試す
    }
  }
  throw new Error('日本語が出るフォントが 1 つも見つかりませんでした');
}

// ── 小さい道具 ──────────────────────────────────────────────
function rgb(hex: string): RGB {
  const n = parseInt(hex.slice(1), 16);
  return { r: ((n >> 16) & 0xff) / 255, g: ((n >> 8) & 0xff) / 255, b: (n & 0xff) / 255 };
}

function solid(hex: string, opacity = 1): SolidPaint {
  return { type: 'SOLID', color: rgb(hex), opacity };
}

/** 縦積みの Auto Layout フレーム。 */
function column(name: string, width: number, gap = 0): FrameNode {
  const f = figma.createFrame();
  f.name = name;
  f.layoutMode = 'VERTICAL';
  f.primaryAxisSizingMode = 'AUTO';
  f.counterAxisSizingMode = 'FIXED';
  f.resize(width, 1);
  f.itemSpacing = gap;
  f.fills = [];
  return f;
}

/** 横並びの Auto Layout フレーム。 */
function row(name: string, width: number, height: number, gap = 0): FrameNode {
  const f = figma.createFrame();
  f.name = name;
  f.layoutMode = 'HORIZONTAL';
  f.primaryAxisSizingMode = 'FIXED';
  f.counterAxisSizingMode = 'FIXED';
  f.resize(width, height);
  f.itemSpacing = gap;
  f.counterAxisAlignItems = 'CENTER';
  f.fills = [];
  return f;
}

type TypeKey = keyof typeof TYPE;

function text(value: string, kind: TypeKey, color: string): TextNode {
  const t = figma.createText();
  const spec = TYPE[kind];
  t.fontName = spec.bold ? fonts.medium : fonts.regular;
  t.fontSize = spec.size;
  t.lineHeight = { unit: 'PIXELS', value: spec.line };
  t.fills = [solid(color)];
  t.characters = value;
  return t;
}

/** 幅いっぱいに伸びる余白。Auto Layout の中で右寄せを作るのに使う。 */
function spacer(parent: FrameNode): FrameNode {
  const s = figma.createFrame();
  s.name = 'spacer';
  s.fills = [];
  s.resize(1, 1);
  parent.appendChild(s);
  s.layoutGrow = 1;
  return s;
}

function divider(width: number): RectangleNode {
  const r = figma.createRectangle();
  r.name = 'Divider';
  r.resize(width, 1);
  r.fills = [solid(M3.outlineVariant)];
  return r;
}

// ── 状態の印（コンポーネント・3 バリアント）────────────────────────
// docs/decisions.md 12・21:
//   未着手 = 薄い破線の円（実線の ○ は「正解」と読まれるので使わない）
//   ✓ = やった / 🔖 = 見直したい（一番目立たせる）
//   色は補助。印だけで区別がつく
type Mark = 'unstarted' | 'done' | 'review';

function markVisual(kind: Mark): FrameNode {
  const box = figma.createFrame();
  box.name = 'mark';
  box.resize(24, 24);
  box.fills = [];
  box.layoutMode = 'HORIZONTAL';
  box.primaryAxisAlignItems = 'CENTER';
  box.counterAxisAlignItems = 'CENTER';
  box.primaryAxisSizingMode = 'FIXED';
  box.counterAxisSizingMode = 'FIXED';

  if (kind === 'unstarted') {
    const e = figma.createEllipse();
    e.name = 'dashed';
    e.resize(18, 18);
    e.fills = [];
    e.strokes = [solid(M3.outlineVariant)];
    e.strokeWeight = 1;
    e.dashPattern = [2, 2];
    box.appendChild(e);
  } else {
    const glyph = kind === 'done' ? '✓' : '🔖';
    const color = kind === 'done' ? M3.onSurfaceVariant : M3.tertiary;
    box.appendChild(text(glyph, 'bodyLarge', color));
  }
  return box;
}

function buildMarkComponent(): ComponentSetNode {
  const variants: ComponentNode[] = (['unstarted', 'done', 'review'] as Mark[]).map((kind) => {
    const c = figma.createComponent();
    c.name = `state=${kind}`;
    c.resize(24, 24);
    c.fills = [];
    c.appendChild(markVisual(kind));
    return c;
  });
  const set = figma.combineAsVariants(variants, figma.currentPage);
  set.name = 'StateMark';
  return set;
}

/**
 * バリアントを名前で引いてインスタンスにする。
 *
 * **見つからなければ例外にする。** `as never` で型を黙らせると、名前を間違えても
 * 型チェックが通り、実行時に `undefined.createInstance()` で落ちる。
 */
function instanceOf(set: ComponentSetNode, variantName: string): InstanceNode {
  const variant = set.children.find((c) => c.name === variantName);
  if (!variant || variant.type !== 'COMPONENT') {
    throw new Error(`${set.name} に ${variantName} がありません`);
  }
  return variant.createInstance();
}

function markInstance(set: ComponentSetNode, kind: Mark): InstanceNode {
  return instanceOf(set, `state=${kind}`);
}

function navInstance(set: ComponentSetNode, active: '教材' | '復習'): InstanceNode {
  return instanceOf(set, `active=${active}`);
}

// ── 画面の骨組み ────────────────────────────────────────────
function screen(name: string): FrameNode {
  const f = figma.createFrame();
  f.name = name;
  f.resize(SCREEN.width, SCREEN.height);
  f.fills = [solid(M3.surface)];
  f.layoutMode = 'VERTICAL';
  f.primaryAxisSizingMode = 'FIXED';
  f.counterAxisSizingMode = 'FIXED';
  f.clipsContent = true;
  return f;
}

function topAppBar(title: string, back: boolean, trailing?: string): FrameNode {
  const bar = row('TopAppBar', SCREEN.width, 64, 8);
  bar.fills = [solid(M3.surfaceContainer)];
  bar.paddingLeft = PAD;
  bar.paddingRight = PAD;
  if (back) bar.appendChild(text('←', 'headlineSmall', M3.onSurface));
  bar.appendChild(text(title, 'headlineSmall', M3.onSurface));
  spacer(bar);
  if (trailing) bar.appendChild(text(trailing, 'headlineSmall', M3.onSurfaceVariant));
  return bar;
}

/** 下部ナビ。`教材` と `復習` の 2 つだけ（decisions: AI をタブにしない）。 */
function buildNavComponent(): ComponentSetNode {
  const variants: ComponentNode[] = (['教材', '復習'] as const).map((active) => {
    const c = figma.createComponent();
    c.name = `active=${active}`;
    c.resize(SCREEN.width, 72);
    c.fills = [solid(M3.surfaceContainer)];
    c.layoutMode = 'HORIZONTAL';
    c.primaryAxisSizingMode = 'FIXED';
    c.counterAxisSizingMode = 'FIXED';
    c.counterAxisAlignItems = 'CENTER';

    for (const label of ['教材', '復習'] as const) {
      const item = column(`tab-${label}`, SCREEN.width / 2, 4);
      item.counterAxisAlignItems = 'CENTER';
      item.paddingTop = 12;
      item.paddingBottom = 12;
      if (label === active) {
        const pill = figma.createFrame();
        pill.name = 'indicator';
        pill.resize(64, 32);
        pill.cornerRadius = 16;
        pill.fills = [solid(M3.primaryContainer)];
        pill.layoutMode = 'HORIZONTAL';
        pill.primaryAxisAlignItems = 'CENTER';
        pill.counterAxisAlignItems = 'CENTER';
        pill.appendChild(text(label === '教材' ? '▤' : '↻', 'bodyLarge', M3.onPrimaryContainer));
        item.appendChild(pill);
        item.appendChild(text(label, 'labelMedium', M3.onSurface));
      } else {
        item.appendChild(text(label === '教材' ? '▤' : '↻', 'bodyLarge', M3.onSurfaceVariant));
        item.appendChild(text(label, 'labelMedium', M3.onSurfaceVariant));
      }
      c.appendChild(item);
    }
    return c;
  });
  const set = figma.combineAsVariants(variants, figma.currentPage);
  set.name = 'NavigationBar';
  return set;
}

/** 本文の領域。ナビの高さぶんを残して縦に伸びる。 */
function body(parent: FrameNode): FrameNode {
  const b = column('body', SCREEN.width, 0);
  parent.appendChild(b);
  b.layoutGrow = 1;
  b.primaryAxisSizingMode = 'FIXED';
  return b;
}

// ── ① 教材一覧 ──────────────────────────────────────────────
function materialRow(title: string, done: number, review: number): FrameNode {
  const r = row('ListItem', SCREEN.width, 72, 0);
  r.paddingLeft = PAD;
  r.paddingRight = PAD;
  const col = column('text', SCREEN.width - PAD * 2, 4);
  col.appendChild(text(title, 'titleMedium', M3.onSurface));
  col.appendChild(text(`学習済み ${done} / 要復習 ${review}`, 'labelMedium', M3.onSurfaceVariant));
  r.appendChild(col);
  return r;
}

function buildMaterialList(nav: ComponentSetNode): FrameNode {
  const s = screen('01 教材一覧');
  s.appendChild(topAppBar('教材', false, '＋'));
  const b = body(s);
  b.appendChild(materialRow('基本情報技術者 午前対策', 32, 12));
  b.appendChild(divider(SCREEN.width));
  b.appendChild(materialRow('TOEIC 文法問題集', 8, 3));
  b.appendChild(divider(SCREEN.width));
  s.appendChild(navInstance(nav, '教材'));
  return s;
}

// ── ツリーの行（②③ で使う）──────────────────────────────────
function treeRow(
  marks: ComponentSetNode,
  label: string,
  level: number,
  kind: Mark | null,
  caret: '▼' | '▶' | '',
): FrameNode {
  const r = row('TreeRow', SCREEN.width, 48, 8);
  r.paddingLeft = PAD + Math.min(level, MAX_INDENT_LEVEL) * INDENT;
  r.paddingRight = PAD;
  r.appendChild(text(caret === '' ? ' ' : caret, 'bodyMedium', M3.onSurfaceVariant));
  r.appendChild(text(label, level === 0 ? 'titleMedium' : 'bodyLarge', M3.onSurface));
  spacer(r);
  if (kind) r.appendChild(markInstance(marks, kind));
  return r;
}

// ── ② 教材追加 — 確認・修正 ──────────────────────────────────
function buildConfirmation(marks: ComponentSetNode): FrameNode {
  const s = screen('02 教材追加 — 確認・修正');
  s.appendChild(topAppBar('確認・修正', true));
  const b = body(s);

  const note = row('note', SCREEN.width, 44, 0);
  note.paddingLeft = PAD;
  note.paddingRight = PAD;
  note.fills = [solid(M3.primaryContainer)];
  note.appendChild(text('解析結果を確認してください。ここで直せます。', 'bodyMedium', M3.onPrimaryContainer));
  b.appendChild(note);

  // ツリーは展開した状態で見せる（Issue #9。折りたたまない）
  b.appendChild(treeRow(marks, '第1章 コンピュータ', 0, null, '▼'));
  b.appendChild(treeRow(marks, '1.1 CPU', 1, null, '▼'));
  b.appendChild(treeRow(marks, '問題1', 2, null, ''));
  b.appendChild(treeRow(marks, '問題2', 2, null, ''));
  b.appendChild(treeRow(marks, '問題3', 2, null, ''));
  b.appendChild(treeRow(marks, '1.2 メモリ', 1, null, '▶'));
  b.appendChild(treeRow(marks, '第2章 ネットワーク', 0, null, '▶'));

  const actions = row('actions', SCREEN.width, 88, 12);
  actions.paddingLeft = PAD;
  actions.paddingRight = PAD;

  const retake = row('TextButton', 104, 40, 0);
  retake.primaryAxisAlignItems = 'CENTER';
  retake.appendChild(text('撮り直す', 'labelLarge', M3.primary));
  actions.appendChild(retake);

  const submit = row('FilledButton', 212, 40, 0);
  submit.cornerRadius = 20;
  submit.fills = [solid(M3.primary)];
  submit.primaryAxisAlignItems = 'CENTER';
  submit.appendChild(text('この内容で登録', 'labelLarge', M3.onPrimary));
  actions.appendChild(submit);

  b.appendChild(actions);
  return s;
}

// ── ③ 教材詳細 ──────────────────────────────────────────────
function filterChips(active: 'すべて' | '要復習だけ'): FrameNode {
  const r = row('FilterChips', SCREEN.width, 56, 8);
  r.paddingLeft = PAD;
  r.paddingRight = PAD;
  for (const label of ['すべて', '要復習だけ'] as const) {
    const chip = row(`FilterChip-${label}`, label === 'すべて' ? 80 : 108, 32, 0);
    chip.cornerRadius = 8;
    chip.primaryAxisAlignItems = 'CENTER';
    if (label === active) {
      chip.fills = [solid(M3.primaryContainer)];
      chip.appendChild(text(label, 'labelLarge', M3.onPrimaryContainer));
    } else {
      chip.fills = [];
      chip.strokes = [solid(M3.outlineVariant)];
      chip.strokeWeight = 1;
      chip.appendChild(text(label, 'labelLarge', M3.onSurfaceVariant));
    }
    r.appendChild(chip);
  }
  return r;
}

function buildDetail(marks: ComponentSetNode, nav: ComponentSetNode): FrameNode {
  const s = screen('03 教材詳細');
  s.appendChild(topAppBar('基本情報技術者', true, '⋯'));
  const b = body(s);
  b.appendChild(filterChips('すべて'));
  b.appendChild(treeRow(marks, '第1章 コンピュータ', 0, null, '▼'));
  b.appendChild(treeRow(marks, '1.1 CPU', 1, null, '▼'));
  b.appendChild(treeRow(marks, '問題1', 2, 'unstarted', ''));
  b.appendChild(treeRow(marks, '問題2', 2, 'done', ''));
  b.appendChild(treeRow(marks, '問題3', 2, 'review', ''));
  b.appendChild(treeRow(marks, '1.2 メモリ', 1, 'review', '▶'));
  b.appendChild(treeRow(marks, '第2章 ネットワーク', 0, 'unstarted', '▶'));
  s.appendChild(navInstance(nav, '教材'));
  return s;
}

// ── ④ 復習一覧 ──────────────────────────────────────────────
// 「最後にやった日」を出すのはこの画面だけ（decisions 20）
function reviewRow(marks: ComponentSetNode, material: string, label: string, when: string): FrameNode {
  const r = row('ListItem', SCREEN.width, 72, 8);
  r.paddingLeft = PAD;
  r.paddingRight = PAD;
  const col = column('text', 260, 2);
  const head = row('head', 260, 24, 8);
  head.appendChild(text(material, 'labelMedium', M3.onSurfaceVariant));
  head.appendChild(text(label, 'bodyLarge', M3.onSurface));
  col.appendChild(head);
  col.appendChild(text(when, 'labelMedium', M3.onSurfaceVariant));
  r.appendChild(col);
  spacer(r);
  r.appendChild(markInstance(marks, 'review'));
  return r;
}

function buildReviewList(marks: ComponentSetNode, nav: ComponentSetNode): FrameNode {
  const s = screen('04 復習一覧');
  s.appendChild(topAppBar('復習', false));
  const b = body(s);
  b.appendChild(reviewRow(marks, '基本情報技術者', '1.2 メモリ', '3日前'));
  b.appendChild(divider(SCREEN.width));
  b.appendChild(reviewRow(marks, '基本情報技術者', '問題12', '2週間前'));
  b.appendChild(divider(SCREEN.width));
  b.appendChild(reviewRow(marks, 'TOEIC 文法', '2.2 時制', '1ヶ月前'));
  b.appendChild(divider(SCREEN.width));
  s.appendChild(navInstance(nav, '復習'));
  return s;
}

// ── 実行 ────────────────────────────────────────────────────
async function main(): Promise<void> {
  fonts = await loadFonts();

  // 既存のページを壊さない（Issue #9）
  const page = figma.createPage();
  page.name = `Revia wireframes ${new Date().toISOString().slice(0, 10)}`;
  await figma.setCurrentPageAsync(page);

  const marks = buildMarkComponent();
  const nav = buildNavComponent();
  // コンポーネントの定義はキャンバスの外に置く
  marks.x = -400;
  marks.y = 0;
  nav.x = -400;
  nav.y = 120;

  const screens = [
    buildMaterialList(nav),
    buildConfirmation(marks),
    buildDetail(marks, nav),
    buildReviewList(marks, nav),
  ];

  screens.forEach((s, i) => {
    page.appendChild(s);
    s.x = i * (SCREEN.width + GAP);
    s.y = 0;
  });

  figma.viewport.scrollAndZoomIntoView(screens);
  figma.notify(`Revia のワイヤーフレーム ${screens.length} 画面を「${page.name}」に生成しました`);
  figma.closePlugin();
}

main().catch((e: unknown) => {
  figma.closePlugin(`生成に失敗しました: ${e instanceof Error ? e.message : String(e)}`);
});
