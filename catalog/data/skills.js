window.CATALOG_CONFIG = {"skills_repo_ssh":"git@github.com:kccchuanchuankang-cell/team-skill-cloud.git","skills_repo_https":"https://github.com/kccchuanchuankang-cell/team-skill-cloud.git","config_note":"可在本文件中覆盖：skills_repo_ssh、skills_repo_https、source_ref（源码链接用的分支或提交）、repo_web_base。若设置 disable_git_origin 为 true，则不再从 git 自动探测。","repo_web_base":"https://github.com/kccchuanchuankang-cell/team-skill-cloud","source_ref":"main","remote_provider":"github","remote_detected":true};
window.SKILLS_DATA = [
    {
        "name":  "backend-api",
        "title":  "Backend API",
        "description":  "Backend API implementation and contract-sensitive server work.",
        "tags":  [
                     "backend",
                     "api",
                     "validation"
                 ],
        "owner":  "platform-team",
        "status":  "active",
        "version":  "0.1.0",
        "summary":  "Use this skill for backend endpoints, handler changes, request validation, and response-shape work where client compatibility matters.",
        "use_cases":  [
                          "Add or update API endpoints",
                          "Change request validation or response formats",
                          "Refactor controller or service layers with contract awareness"
                      ],
        "install_hint":  "Install for API services and full-stack projects that expose backend endpoints.",
        "skill_path":  "skills/backend-api/SKILL.md",
        "meta_path":  "skills/backend-api/meta.json",
        "skill_web_url":  "https://github.com/kccchuanchuankang-cell/team-skill-cloud/blob/main/skills/backend-api/SKILL.md",
        "meta_web_url":  "https://github.com/kccchuanchuankang-cell/team-skill-cloud/blob/main/skills/backend-api/meta.json"
    },
    {
        "name":  "frontend-react",
        "title":  "Frontend React",
        "description":  "React UI implementation aligned to team frontend conventions.",
        "tags":  [
                     "frontend",
                     "react",
                     "ui"
                 ],
        "owner":  "frontend-team",
        "status":  "active",
        "version":  "0.1.0",
        "summary":  "Use this skill for React components, pages, client-side interactions, and UI changes that should match existing product patterns.",
        "use_cases":  [
                          "Build or refine React pages and components",
                          "Add forms, state transitions, and edge states",
                          "Keep new UI aligned with surrounding design patterns"
                      ],
        "install_hint":  "Install for React applications and full-stack projects with product UI work.",
        "skill_path":  "skills/frontend-react/SKILL.md",
        "meta_path":  "skills/frontend-react/meta.json",
        "skill_web_url":  "https://github.com/kccchuanchuankang-cell/team-skill-cloud/blob/main/skills/frontend-react/SKILL.md",
        "meta_web_url":  "https://github.com/kccchuanchuankang-cell/team-skill-cloud/blob/main/skills/frontend-react/meta.json"
    },
    {
        "name":  "release-triage",
        "title":  "Release Triage",
        "description":  "Release readiness, regression scanning, and risk-focused review.",
        "tags":  [
                     "release",
                     "review",
                     "qa"
                 ],
        "owner":  "platform-team",
        "status":  "active",
        "version":  "0.1.0",
        "summary":  "Use this skill when you want a focused pass on release risk, user-visible regressions, missing validation, and rollback complexity.",
        "use_cases":  [
                          "Review a release candidate for regressions",
                          "Triage risky changes or incidents before shipping",
                          "Call out testing gaps and rollback risk"
                      ],
        "install_hint":  "Install for projects that want a reusable pre-release or regression review workflow.",
        "skill_path":  "skills/release-triage/SKILL.md",
        "meta_path":  "skills/release-triage/meta.json",
        "skill_web_url":  "https://github.com/kccchuanchuankang-cell/team-skill-cloud/blob/main/skills/release-triage/SKILL.md",
        "meta_web_url":  "https://github.com/kccchuanchuankang-cell/team-skill-cloud/blob/main/skills/release-triage/meta.json"
    }
];

