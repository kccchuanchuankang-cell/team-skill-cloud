const skills = Array.isArray(window.SKILLS_DATA) ? window.SKILLS_DATA : [];

/** UI 文案为简体中文；技能标题/描述仍来自 meta.json */

function tagLabel(tag) {
  return tag === "all" ? "全部" : tag;
}

const rawCatalogConfig =
  window.CATALOG_CONFIG && typeof window.CATALOG_CONFIG === "object" ? window.CATALOG_CONFIG : {};

const catalogConfig = {
  skills_repo_ssh: rawCatalogConfig.skills_repo_ssh || "git@github.com:your-org/skills-cloud.git",
  skills_repo_https: rawCatalogConfig.skills_repo_https || "https://github.com/your-org/skills-cloud.git",
  config_note: rawCatalogConfig.config_note || "",
  repo_web_base: rawCatalogConfig.repo_web_base || "",
  source_ref: rawCatalogConfig.source_ref || "",
  remote_detected: Boolean(rawCatalogConfig.remote_detected)
};

const state = {
  search: "",
  tag: "all",
  selected: skills[0]?.name ?? null
};

/** 窄屏下用户点击卡片后，下一次 render 结束时将详情滚入视口 */
let scrollDetailIntoViewNext = false;

const elements = {
  count: document.getElementById("skill-count"),
  search: document.getElementById("search-input"),
  tags: document.getElementById("tag-filters"),
  list: document.getElementById("skill-list"),
  listResultCount: document.getElementById("list-result-count"),
  detail: document.getElementById("skill-detail"),
  detailColumn: document.getElementById("detail-column")
};

function initSkillFromUrl() {
  try {
    const params = new URLSearchParams(window.location.search);
    let name = params.get("skill")?.trim();
    if (!name && window.location.hash.length > 1) {
      name = window.location.hash.slice(1).replace(/^skill[=:]/i, "").trim();
    }
    if (name && skills.some((s) => s.name === name)) {
      state.selected = name;
    }
  } catch {
    /* ignore */
  }
}

function syncSkillToUrl() {
  try {
    const url = new URL(window.location.href);
    url.hash = "";
    if (state.selected && skills.some((s) => s.name === state.selected)) {
      url.searchParams.set("skill", state.selected);
    } else {
      url.searchParams.delete("skill");
    }
    const next = `${url.pathname}${url.search}`;
    const cur = `${window.location.pathname}${window.location.search}`;
    if (next !== cur) {
      window.history.replaceState(null, "", next);
    }
  } catch {
    /* ignore */
  }
}

function updateListResultCount(items) {
  if (!elements.listResultCount) {
    return;
  }
  const total = skills.length;
  const n = items.length;
  const filtered = Boolean(state.search) || state.tag !== "all";
  if (!filtered) {
    elements.listResultCount.textContent = "";
    return;
  }
  if (n === 0) {
    elements.listResultCount.textContent = `共 ${total} 项 · 无匹配`;
    return;
  }
  elements.listResultCount.textContent = `共 ${total} 项 · 当前显示 ${n} 项`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function repositoryPathRow(relPath, webUrl) {
  const pathStr = String(relPath ?? "");
  const url = webUrl && /^https?:\/\//i.test(String(webUrl)) ? String(webUrl) : "";
  if (url) {
    return `<a class="path-link" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(pathStr)}</a>`;
  }
  return `<span class="path-link path-plain">${escapeHtml(pathStr)}</span>`;
}

/** 解析为 OpenSkills 支持的 GitHub 简写 owner/repo（用于 owner/repo/skills/技能名） */
function deriveGithubShorthand() {
  const https = String(catalogConfig.skills_repo_https || "").trim();
  try {
    const normalized = https.replace(/\.git$/i, "");
    const u = new URL(normalized);
    if (u.hostname.toLowerCase() !== "github.com") {
      return null;
    }
    const seg = u.pathname.split("/").filter(Boolean);
    if (seg.length >= 2) {
      return `${seg[0]}/${seg[1]}`;
    }
  } catch {
    /* ignore */
  }
  const ssh = String(catalogConfig.skills_repo_ssh || "").trim();
  const m = ssh.match(/^git@github\.com:([^/]+)\/(.+)$/i);
  if (!m) {
    return null;
  }
  const repo = m[2].replace(/\.git$/i, "");
  return `${m[1]}/${repo}`;
}

function buildOpenskillsSection(skillName) {
  const ssh = catalogConfig.skills_repo_ssh;
  const https = catalogConfig.skills_repo_https;
  const installSsh = `npx openskills install ${ssh}`;
  const installHttps = `npx openskills install ${https}`;
  const sync = "npx openskills sync";
  const universalSsh = `npx openskills install ${ssh} --universal`;
  const readCmd = `npx openskills read ${skillName}`;
  const gh = deriveGithubShorthand();
  const singlePath = gh ? `${gh}/skills/${skillName}` : "";

  const noteBlock = catalogConfig.config_note
    ? `<p class="cmd-note">${escapeHtml(catalogConfig.config_note)}</p>`
    : "";

  const block = (label, text) => `
    <p class="cmd-label">${escapeHtml(label)}</p>
    <div class="cmd-row">
      <pre class="cmd-snippet"><code>${escapeHtml(text)}</code></pre>
      <button type="button" class="cmd-copy" data-copy-payload="${escapeHtml(encodeURIComponent(text))}">复制</button>
    </div>
  `;

  const nonGithubNote = `<p class="cmd-note">仓库地址不是 GitHub 时，可先 <code>git clone</code> 本仓，再 <code>npx openskills install ./&lt;克隆路径&gt;/skills/${escapeHtml(
    skillName
  )}</code> 与 <code>npx openskills sync</code>；单技能路径格式为 <code>owner/repo/skills/技能目录名</code>。</p>`;

  const moreInner = [
    block("安装整个技能仓（SSH）", `${installSsh}\n${sync}`),
    block("安装整个技能仓到 .agent/skills（--universal）", `${universalSsh}\n${sync}`),
    gh
      ? block("仅本技能到 .agent/skills", `npx openskills install ${singlePath} --universal\n${sync}`)
      : nonGithubNote
  ].join("");

  return `
    <div class="detail-section">
      <h3>OpenSkills</h3>
      ${gh ? block("仅安装本技能", `npx openskills install ${singlePath}\n${sync}`) : ""}
      ${block("安装整个技能仓（HTTPS，推荐）", `${installHttps}\n${sync}`)}
      ${block("在终端加载本技能", readCmd)}
      <details class="cmd-more">
        <summary>更多安装方式（SSH、通用路径等）</summary>
        <div class="cmd-more-body">
          ${moreInner}
        </div>
      </details>
    </div>
  `;
}

function uniqueValues(items) {
  return [...new Set(items)].sort((a, b) => a.localeCompare(b));
}

let tagFiltersBuilt = false;

function buildTagFiltersOnce() {
  if (tagFiltersBuilt) {
    return;
  }

  const tags = uniqueValues(skills.flatMap((skill) => skill.tags || []));
  const allTags = ["all", ...tags];

  elements.tags.innerHTML = "";

  allTags.forEach((tag) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "tag-button";
    button.dataset.tag = tag;
    button.textContent = tagLabel(tag);
    button.addEventListener("click", () => {
      state.tag = tag;
      render();
    });
    elements.tags.appendChild(button);
  });

  tagFiltersBuilt = true;
}

function updateTagFilterActive() {
  [...elements.tags.children].forEach((button) => {
    button.classList.toggle("is-active", button.dataset.tag === state.tag);
  });
}

function matchesSearch(skill, search) {
  if (!search) {
    return true;
  }

  const haystack = [
    skill.name,
    skill.title,
    skill.description,
    skill.summary,
    skill.owner,
    ...(skill.tags || []),
    ...(skill.use_cases || [])
  ]
    .join(" ")
    .toLowerCase();

  return haystack.includes(search.toLowerCase());
}

function filteredSkills() {
  return skills.filter((skill) => {
    const tagMatch = state.tag === "all" || (skill.tags || []).includes(state.tag);
    const searchMatch = matchesSearch(skill, state.search);
    return tagMatch && searchMatch;
  });
}

function ensureSelection(items) {
  if (!items.length) {
    state.selected = null;
    return;
  }

  const stillVisible = items.some((skill) => skill.name === state.selected);
  if (!stillVisible) {
    state.selected = items[0].name;
  }
}

function renderList(items) {
  elements.list.innerHTML = "";

  if (!items.length) {
    const empty = document.createElement("div");
    empty.className = "no-results";
    empty.textContent = "没有符合当前筛选条件的技能。";
    elements.list.appendChild(empty);
    return;
  }

  items.forEach((skill) => {
    const card = document.createElement("article");
    card.className = `skill-card${state.selected === skill.name ? " is-selected" : ""}`;
    card.tabIndex = 0;
    card.setAttribute("role", "button");
    card.setAttribute("aria-pressed", state.selected === skill.name ? "true" : "false");
    card.setAttribute("aria-label", `查看技能：${skill.title}`);
    card.innerHTML = `
      <div class="card-head">
        <h2>${escapeHtml(skill.title)}</h2>
        <span class="meta-pill meta-pill-version">v${escapeHtml(skill.version)}</span>
      </div>
      <p class="card-desc">${escapeHtml(skill.description)}</p>
      <div class="meta-row">
        ${(skill.tags || []).map((tag) => `<span class="meta-pill">${escapeHtml(tag)}</span>`).join("")}
      </div>
      <div class="meta-row meta-row--tight">
        <span class="meta-inline"><span class="meta-inline-label">负责人</span> ${escapeHtml(skill.owner)}</span>
        <span class="meta-inline meta-inline-mono">${escapeHtml(skill.name)}</span>
      </div>
    `;
    card.addEventListener("click", () => {
      state.selected = skill.name;
      scrollDetailIntoViewNext = window.matchMedia("(max-width: 920px)").matches;
      render();
    });
    card.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        state.selected = skill.name;
        scrollDetailIntoViewNext = window.matchMedia("(max-width: 920px)").matches;
        render();
      }
    });
    elements.list.appendChild(card);
  });
}

function renderDetail(items) {
  const selected = items.find((skill) => skill.name === state.selected);

  if (!selected) {
    elements.detail.className = "detail-card empty-state";
    elements.detail.innerHTML = `
      <h2>未选中技能</h2>
      <p>${items.length === 0 ? "没有符合当前筛选的技能，请调整搜索或筛选条件。" : "请在列表中选择一项技能。"}</p>
    `;
    return;
  }

  elements.detail.className = "detail-card";
  elements.detail.innerHTML = `
    <div class="detail-head">
      <h2>${escapeHtml(selected.title)}</h2>
      <span class="meta-pill meta-pill-version">v${escapeHtml(selected.version)}</span>
    </div>
    <p class="detail-summary">${escapeHtml(selected.summary)}</p>
    <div class="meta-row">
      ${(selected.tags || []).map((tag) => `<span class="detail-tag">${escapeHtml(tag)}</span>`).join("")}
    </div>
    <div class="detail-section">
      <h3>适用场景</h3>
      <ul>
        ${(selected.use_cases || []).map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
      </ul>
    </div>
    <div class="detail-section">
      <h3>安装说明</h3>
      <p class="install-hint">${escapeHtml(selected.install_hint)}</p>
    </div>
    ${buildOpenskillsSection(selected.name)}
    <div class="detail-section">
      <h3>负责人与目录名</h3>
      <p class="detail-meta-line">
        <span>${escapeHtml(selected.owner)}</span>
        <span class="detail-meta-sep" aria-hidden="true">·</span>
        <code class="skill-id-code">${escapeHtml(selected.name)}</code>
      </p>
    </div>
    <div class="detail-section">
      <h3>仓库路径</h3>
      ${
        catalogConfig.remote_detected && catalogConfig.repo_web_base
          ? `<p class="path-hint">链接使用生成时的引用 <code>${escapeHtml(catalogConfig.source_ref)}</code>，仓库 <code>${escapeHtml(catalogConfig.repo_web_base)}</code>（来自当时 Git 状态）。</p>`
          : `<p class="path-hint">路径相对于技能仓根目录。在 Git 克隆内运行 <code>generate-catalog.ps1</code> 可生成指向源码托管站的链接。</p>`
      }
      <div class="path-list">
        ${repositoryPathRow(selected.skill_path, selected.skill_web_url)}
        ${repositoryPathRow(selected.meta_path, selected.meta_web_url)}
      </div>
    </div>
  `;
}

function renderStats() {
  if (elements.count) {
    elements.count.textContent = skills.length.toString();
  }
}

function render() {
  buildTagFiltersOnce();
  updateTagFilterActive();
  const items = filteredSkills();
  ensureSelection(items);
  renderList(items);
  renderDetail(items);
  updateListResultCount(items);
  syncSkillToUrl();
  if (scrollDetailIntoViewNext && elements.detailColumn) {
    scrollDetailIntoViewNext = false;
    const instant = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    elements.detailColumn.scrollIntoView({
      behavior: instant ? "auto" : "smooth",
      block: "nearest"
    });
  }
}

function debounce(fn, ms) {
  let timeoutId = null;
  return () => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(fn, ms);
  };
}

const scheduleRenderFromSearch = debounce(() => render(), 120);

elements.search.addEventListener("input", (event) => {
  state.search = event.target.value.trim();
  scheduleRenderFromSearch();
});

if (elements.detailColumn) {
  elements.detailColumn.addEventListener("click", (event) => {
    const button = event.target.closest(".cmd-copy");
    if (!button || !elements.detailColumn.contains(button)) {
      return;
    }

    const encoded = button.getAttribute("data-copy-payload");
    const text = encoded ? decodeURIComponent(encoded) : button.getAttribute("data-copy");
    if (!text) {
      return;
    }

    navigator.clipboard.writeText(text).then(
      () => {
        const previous = button.textContent;
        button.textContent = "已复制";
        window.setTimeout(() => {
          button.textContent = previous;
        }, 1600);
      },
      () => {}
    );
  });
}

initSkillFromUrl();
renderStats();
render();
