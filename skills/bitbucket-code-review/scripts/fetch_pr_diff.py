"""
获取 Bitbucket PR 的 diff 信息
轻量级工具，只负责下载 PR 的变更内容，由 AI 进行智能审查

使用方法:
    python fetch_pr_diff.py <pr_url> [--api-token TOKEN] [--username USERNAME]
    
    或使用环境变量:
    set BITBUCKET_API_TOKEN=your_token
    set BITBUCKET_USERNAME=your_username
    python fetch_pr_diff.py <pr_url>
    
示例:
    python fetch_pr_diff.py https://bitbucket.org/your-workspace/your-repo/pull-requests/42 --api-token ATATT... --username user@example.com
"""

import sys
import re
import json
import os
import argparse
from pathlib import Path
from typing import Tuple, Optional
from datetime import datetime
from bitbucket_pr_reader import BitbucketPRReader


def parse_pr_url(pr_url: str) -> Tuple[str, str, int]:
    """
    解析 PR URL
    
    Args:
        pr_url: PR URL，格式如 https://bitbucket.org/your-workspace/your-repo/pull-requests/42
        
    Returns:
        (workspace, repo_slug, pr_id)
    """
    pattern = r'bitbucket\.org/([^/]+)/([^/]+)/pull-requests?/(\d+)'
    match = re.search(pattern, pr_url)
    
    if not match:
        raise ValueError(f"无法解析PR URL: {pr_url}")
    
    workspace = match.group(1)
    repo_slug = match.group(2)
    pr_id = int(match.group(3))
    
    return workspace, repo_slug, pr_id


def get_credentials() -> Tuple[Optional[str], Optional[str], str]:
    """获取认证信息与 PR URL（命令行参数或环境变量）。"""
    # 解析命令行参数
    parser = argparse.ArgumentParser(
        description='获取 Bitbucket PR 的 diff 信息',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python fetch_pr_diff.py https://bitbucket.org/your-workspace/your-repo/pull-requests/42 --api-token ATATT... --username user@example.com
  
使用环境变量:
  set BITBUCKET_API_TOKEN=your_token
  set BITBUCKET_USERNAME=your_username
  python fetch_pr_diff.py https://bitbucket.org/your-workspace/your-repo/pull-requests/42
        """
    )
    parser.add_argument('pr_url', help='Bitbucket PR URL')
    parser.add_argument('--api-token', help='Bitbucket API Token (或使用环境变量 BITBUCKET_API_TOKEN)')
    parser.add_argument('--username', help='Bitbucket 用户名/邮箱 (或使用环境变量 BITBUCKET_USERNAME)')
    
    args = parser.parse_args()
    
    # 优先使用命令行参数，其次使用环境变量
    api_token = args.api_token or os.environ.get('BITBUCKET_API_TOKEN')
    username = args.username or os.environ.get('BITBUCKET_USERNAME')
    
    return api_token, username, args.pr_url


def main():
    """主函数"""
    # 获取认证信息和 PR URL
    try:
        api_token, username, pr_url = get_credentials()
    except SystemExit:
        sys.exit(1)
    
    # 验证认证信息
    if not api_token:
        print("错误: 缺少 API Token")
        print("请通过以下任一方式提供:")
        print("  1. 命令行参数: --api-token YOUR_TOKEN")
        print("  2. 环境变量: set BITBUCKET_API_TOKEN=YOUR_TOKEN")
        sys.exit(1)
    
    if not username:
        print("警告: 未提供用户名")
        print("某些 API Token 类型需要配合用户名使用")
        print("如果遇到认证错误，请通过以下任一方式提供用户名:")
        print("  1. 命令行参数: --username YOUR_USERNAME")
        print("  2. 环境变量: set BITBUCKET_USERNAME=YOUR_USERNAME")
        print()
    
    print("=" * 80)
    print("Bitbucket PR Diff 获取工具")
    print("=" * 80)
    
    # 1. 解析 PR URL
    print(f"\n[1/3] 解析 PR URL...")
    try:
        workspace, repo_slug, pr_id = parse_pr_url(pr_url)
        print(f"  ✓ 仓库: {workspace}/{repo_slug}")
        print(f"  ✓ PR ID: {pr_id}")
    except ValueError as e:
        print(f"  ✗ 错误: {e}")
        sys.exit(1)
    
    # 2. 获取 PR 信息
    print(f"\n[2/3] 获取 PR 信息...")
    
    reader = BitbucketPRReader(api_token=api_token, username=username)
    
    pr_info = reader.get_pr_info(workspace, repo_slug, pr_id)
    if not pr_info:
        print("  ✗ 无法获取 PR 信息")
        sys.exit(1)
    
    print(f"  ✓ PR标题: {pr_info['title']}")
    print(f"  ✓ 作者: {pr_info['author']['display_name']}")
    print(f"  ✓ 状态: {pr_info['state']}")
    
    # 获取提交信息
    commits = reader.get_pr_commits(workspace, repo_slug, pr_id)
    print(f"  ✓ 提交数: {len(commits)}")
    
    # 3. 创建输出文件夹（添加时间戳）
    timestamp = datetime.now().strftime("%Y%m%d%H%M")
    output_dir = f"{repo_slug}_PR{pr_id}_{timestamp}"
    print(f"\n[3/4] 创建输出文件夹...")
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    print(f"  ✓ 输出文件夹: {output_dir}/")
    
    # 4. 获取 diff
    print(f"\n[4/4] 下载代码变更...")
    diff_content = reader.get_pr_diff(workspace, repo_slug, pr_id)
    
    if not diff_content:
        print("  ✗ 无法获取 diff 内容")
        sys.exit(1)
    
    # 保存 diff 到文件
    diff_filename = f"{repo_slug}_{pr_id}_{timestamp}_diff.txt"
    diff_path = os.path.join(output_dir, diff_filename)
    with open(diff_path, 'w', encoding='utf-8') as f:
        f.write(diff_content)
    
    print(f"  ✓ Diff大小: {len(diff_content)} 字符")
    print(f"  ✓ 已保存到: {diff_path}")
    
    # 保存 PR 信息到 JSON
    info_filename = f"{repo_slug}_{pr_id}_{timestamp}_info.json"
    info_path = os.path.join(output_dir, info_filename)
    pr_data = {
        'workspace': workspace,
        'repo_slug': repo_slug,
        'pr_id': pr_id,
        'pr_info': pr_info,
        'commits': commits,
        'pr_url': pr_url
    }
    
    with open(info_path, 'w', encoding='utf-8') as f:
        json.dump(pr_data, f, indent=2, ensure_ascii=False)
    
    print(f"  ✓ PR信息已保存到: {info_path}")
    
    # 输出摘要
    print("\n" + "=" * 80)
    print("下载完成")
    print("=" * 80)
    print(f"\n输出文件夹: {output_dir}/")
    print(f"\n生成的文件:")
    print(f"  1. {diff_filename} - 代码变更 diff")
    print(f"  2. {info_filename} - PR 详细信息")
    print(f"\n下一步:")
    print(f"  请 AI 助手阅读 {output_dir}/ 文件夹中的文件以及编码规范文档，")
    print(f"  进行智能代码审查，并将审查报告保存到同一文件夹中")
    print("=" * 80)


if __name__ == "__main__":
    main()
