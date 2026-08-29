# Tooling Rationale — Why This Stack Exists

Grounded in Mem notes (titles cited) + `~/.config/opencode/opencode.json`.
Where a rationale is not in Mem, it is marked **[inferred]**.

## The umbrella goal

Mem notes "Portfolio Site — Project Refresher (2026-08-23)" and "Mother Folder Knowledge
Graph — Refresher (2026-08-23)" define the context: an **8-week CV plan** building one
mother folder `D:\app\prject cv` with four projects — `prtfolio` (zero-build static
portfolio), `p2` (git-stats Java CLI), `p3` (task-api FastAPI + Postgres on Railway),
`p4` (TaskVault, Kotlin/Compose offline-first Android client of p3).

Every tool below exists to let a single AI agent run that multi-project, multi-week
build across sessions without losing state. The stack layers into five concerns:

1. **Process discipline** — superpowers skills (spec → plan → execute → verify)
2. **Minimalism enforcement** — ponytail
3. **Codebase navigation** — graphify (+ commit hook)
4. **Persistent memory** — Mem + MemPalace + Screenpipe + clipboard
5. **I/O & infra** — readseek, firecrawl, chrome-devtools, TokenRouter

---

## 1. Process skills — `superpowers` plugin

**Why:** Mem shows the project is run *spec-driven*. The mother-folder note lists
`.superpowers/sdd/taskvault/` — **46 SDD briefs/reports/review-packages logging
TaskVault's 18-task build**. That is the smoking gun: plans and reports are the
durable state between sessions, so the agent can resume days later.

Skill → role in that loop:

- `using-superpowers` — forces skill-check before every action (discipline gate)
- `brainstorming` — requirements/design before code
- `writing-plans` → `executing-plans` — spec → actionable plan → run it
- `subagent-driven-development` / `dispatching-parallel-agents` — delegate plan units
- `test-driven-development` — red/green/refactor (p2/p3 show `mvn test` / pytest verification)
- `systematic-debugging` — root-cause-first (the Canvas-UI "clicking does nothing" note used this pattern)
- `verification-before-completion` — evidence before "done" (matches the "Verification:" blocks in Mem notes)
- `requesting-code-review` / `receiving-code-review` — review handoff
- `using-git-worktrees` / `finishing-a-development-branch` — isolation & integration (p3 used `.worktrees/`)

**Evidence:** "Task API Over-Engineering Cleanup", "Session: Canvas UI Ripple + ponytail audit",
"TaskVault Task 13 complete … (repo p4)".

---

## 2. Minimalism — `@dietrichgebert/ponytail`

**Why:** Mem has **two dedicated over-engineering cleanup sessions**:
- "Task API Over-Engineering Cleanup — Summary": deleted `.venv/`, caches, `.superpowers/`,
  `docs/` (~140k lines) from p3; consolidated helpers; corrected an over-delete mistake.
- "Session: … ponytail audit applied": whole-folder audit, ~95 lines cut across p2/p3/prtfolio,
  deleted dead CSS/JS, removed a byte-identical PUT route + tests.

The user has an explicit "zero dependencies / lean" preference — the portfolio is
"**Zero dependencies**, no build step" by design. `ponytail` encodes YAGNI/stdlib-first as a
*standing mode*, not a one-off audit. Companion skills (`ponytail-review`, `-audit`,
`-debt`, `-gain`) turn that into a recurring review/ledger loop.

---

## 3. Codebase navigation — `graphify`

**Why:** "Mother Folder Knowledge Graph — Refresher (2026-08-23)" records running graphify
over the whole `D:\app\prject cv` folder (193 files → 1,059 nodes · 2,061 edges · 55
communities), producing `graph.html` / `GRAPH_REPORT.md` / `graph.json`. The p4 session
built the same graph for this repo incrementally.

Purpose evidenced in that note:
- Cross-repo tracing, e.g. the confirmed hyperedge **Room write → PendingOp queue →
  SyncWorker → Retrofit drain → p3 task-api** (EXTRACTED 0.95).
- Community detection that splits the folder cleanly by project and by TaskVault feature.

`graphify hook install` adds a post-commit/post-checkout auto-rebuild, so the graph
stays current without manual re-export. This is the "resume-and-orient" layer for an
agent that keeps coming back to a big folder.

---

## 4. Persistent memory — Mem + MemPalace + Screenpipe + clipboard

**Why:** "Screenpipe Activity — 2026-08-18" documents the memory pipeline:
- **Screenpipe → Ollama summarize → Mem note**, scheduled every 2h.
- **Mem MCP bridge** loads on restart via API key (no OAuth), with a manual `/mcp`
  OAuth step acknowledged as a known blocker.
- **MemPalace Identity** configured for a local-first knowledge-graph memory.

`AGENTS.md` then mandates `kg_query`/`kg_add` on **every** turn, plus opportunistic
clipboard capture. The intent: the agent automatically files what the user copies or
does, so memory builds itself instead of the user pasting notes manually.

These are the "doesn't-forget" + "captures-as-you-work" layers.

---

## 5. I/O & infra

| Tool | Config source | Why (rated) |
|---|---|---|
| `opencode-readseek` plugin | `opencode.json` `plugin[]` | `readseek_digest/grep/edit` with `LINE:HASH` anchors — reliable anchored file I/O for edits/replacements **[inferred]** |
| `firecrawl` MCP | `mcp.firecrawl` | web search/scrape/crawl/research; MEM confirmed via "Firecrawl" key note |
| `chrome-devtools` MCP | `mcp.chrome-devtools` | browser automation/network/trace. Note the Mem caveat: **no Chrome installed (only Edge WebView2)** — "verify in Zen" — so full browser verification was desired but constrained **[partially inferred]** |
| `bun-shim.js` local plugin | `plugin[]` `file:///…/bun-shim.js` | runtime shim for bun **[inferred]** |
| `TokenRouter` provider | `provider.tokenrouter` + `small_model` | multi-model router (Claude/DeepSeek/GPT/Gemini/GLM/Kimi/Grok) + a free small model for cheap subtasks. "API Keys and Tokens" Mem note confirms OpenRouter/Ollama/Zai/NVIDIA keys in use |

---

## Installation (the above, concretely)

- **Ponytail**: `opencode plugin add @dietrichgebert/ponytail` (or list it in `plugin[]`)
- **Superpowers**: npm-install into `~/.opencode/plugins/node_modules/superpowers`, reference the abs dir in `plugin[]`
- **Graphify**: `uv tool install graphifyy`; then `graphify install --platform opencode` (skill + AGENTS.md hook + `.opencode` plugin) and `graphify hook install` (git hooks)
- **Mem / MemPalace / Screenpipe / Firecrawl / Chrome-devtools / Clipboard**: all are entries under `mcp` in `opencode.json` (local `command` spawns or remote `url`)
- **Standalone skills** (architecture-lens, crawl4ai, graphify, improve-code-architecture, customize-opencode, workflow-optimizer): drop a `SKILL.md` with `name`/`description` frontmatter into `~/.config/opencode/skills/<name>/` (or `~/.agents/skills`, `~/.claude/skills`)

---

## ⚠ Security flags (must fix)

1. `~/.config/opencode/opencode.json` stores **plaintext API keys** inline
   (TokenRouter `apiKey`, Firecrawl, Mem bearer, Screenpipe key). Committing/sharing
   this file leaks them.
2. A Mem note titled **"API Keys and Tokens"** holds an even larger set in plaintext —
   OpenRouter (3 keys incl. a master key), GitHub PAT, NVIDIA, Ollama, Zai, Alpaca.

Recommend: move secrets to environment variables / a secret manager, delete the plaintext
Mem note, and gitignore `opencode.json` if it currently tracks secrets.