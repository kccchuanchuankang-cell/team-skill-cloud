const skills = Array.isArray(window.SKILLS_DATA) ? window.SKILLS_DATA : [];

/** UI 文案为简体中文；技能标题/描述仍来自 meta.json */
const STATUS_LABELS = {
  active: "启用",
  draft: "草案",
  deprecated: "已废弃",
  archived: "已归档"
};

function statusLabel(status) {
  return STATUS_LABELS[status] || status;
}

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
  status: "all",
  tag: "all",
  selected: skills[0]?.name ?? null
};

const elements = {
  count: document.getElementById("skill-count"),
  owners: document.getElementById("owner-count"),
  search: document.getElementById("search-input"),
  status: document.getElementById("status-filter"),
  tags: document.getElementById("tag-filters"),
  list: document.getElementById("skill-list"),
  detail: document.getElementById("skill-detail"),
  detailColumn: document.getElementById("detail-column")
};

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

function buildOpenskillsSection(skillName) {
  const ssh = catalogConfig.skills_repo_ssh;
  const https = catalogConfig.skills_repo_https;
  const installSsh = `npx openskills install ${ssh}`;
  const installHttps = `npx openskills install ${https}`;
  const sync = "npx openskills sync";
  const universalSsh = `npx openskills install ${ssh} --universal`;
  const readCmd = `npx openskills read ${skillName}`;

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

  return `
    <div class="detail-section">
      <h3>OpenSkills 命令</h3>
      ${noteBlock}
      ${block("安装技能仓（SSH，项目内）", `${installSsh}\n${sync}`)}
      ${block("安装技能仓（HTTPS，项目内）", `${installHttps}\n${sync}`)}
      ${block("通用路径（.agent/skills）", `${universalSsh}\n${sync}`)}
      ${block("在终端中加载本技能", readCmd)}
    </div>
  `;
}

function uniqueValues(items) {
  return [...new Set(items)].sort((a, b) => a.localeCompare(b));
}

function renderStatusOptions() {
  const statuses = uniqueValues(skills.map((skill) => skill.status).filter(Boolean));

  statuses.forEach((status) => {
    const option = document.createElement("option");
    option.value = status;
    option.textContent = statusLabel(status);
    elements.status.appendChild(option);
  });
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
    const statusMatch = state.status === "all" || skill.status === state.status;
    const searchMatch = matchesSearch(skill, state.search);
    return tagMatch && statusMatch && searchMatch;
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
    card.innerHTML = `
      <div class="card-topline">
        <span class="pill">${escapeHtml(statusLabel(skill.status))}</span>
        <span class="meta-pill">v${escapeHtml(skill.version)}</span>
      </div>
      <h2>${escapeHtml(skill.title)}</h2>
      <p>${escapeHtml(skill.description)}</p>
      <div class="meta-row">
        ${(skill.tags || []).map((tag) => `<span class="meta-pill">${escapeHtml(tag)}</span>`).join("")}
      </div>
      <div class="meta-row">
        <span class="meta-pill">负责人：${escapeHtml(skill.owner)}</span>
        <span class="meta-pill">${escapeHtml(skill.name)}</span>
      </div>
    `;
    card.addEventListener("click", () => {
      state.selected = skill.name;
      render();
    });
    card.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        state.selected = skill.name;
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
      <p>${items.length === 0 ? "没有符合当前筛选的技能，请调整搜索或筛选条件。" : "请在左侧选择一项技能。"}</p>
    `;
    return;
  }

  elements.detail.className = "detail-card";
  elements.detail.innerHTML = `
    <div class="detail-topline">
      <span class="pill">${escapeHtml(statusLabel(selected.status))}</span>
      <span class="meta-pill">v${escapeHtml(selected.version)}</span>
    </div>
    <h2>${escapeHtml(selected.title)}</h2>
    <p>${escapeHtml(selected.summary)}</p>
    <div class="meta-row">
      ${(selected.tags || []).map((tag) => `<span class="detail-tag">${escapeHtml(tag)}</span>`).join("")}
    </div>
    <div class="detail-section">
      <h3>适用场景</h3>
      <ul>
        ${(selected.use_cases || []).map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
      </ul>
    </div>
    ${buildOpenskillsSection(selected.name)}
    <div class="detail-section">
      <h3>安装说明</h3>
      <p><code>${escapeHtml(selected.install_hint)}</code></p>
    </div>
    <div class="detail-section">
      <h3>负责人</h3>
      <p>${escapeHtml(selected.owner)}</p>
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
  elements.count.textContent = skills.length.toString();
  elements.owners.textContent = uniqueValues(skills.map((skill) => skill.owner).filter(Boolean)).length.toString();
}

function render() {
  buildTagFiltersOnce();
  updateTagFilterActive();
  const items = filteredSkills();
  ensureSelection(items);
  renderList(items);
  renderDetail(items);
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

elements.status.addEventListener("change", (event) => {
  state.status = event.target.value;
  render();
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

renderStatusOptions();
renderStats();
render();
