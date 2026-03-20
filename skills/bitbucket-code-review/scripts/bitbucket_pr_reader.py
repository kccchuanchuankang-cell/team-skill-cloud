"""
Bitbucket PR Reader
读取Bitbucket仓库中的Pull Request信息（只读模式）
"""

import requests
from typing import Dict, Any, List
import json


class BitbucketPRReader:
    """Bitbucket PR读取器 - 只读模式"""
    
    def __init__(self, api_token: str = None, username: str = None, app_password: str = None):
        """
        初始化Bitbucket PR读取器
        
        Args:
            api_token: Bitbucket API Token (多种格式都支持)
            username: Bitbucket用户名或邮箱（使用某些token类型时需要）
            app_password: Bitbucket App Password（已废弃）
        """
        self.base_url = "https://api.bitbucket.org/2.0"
        self.session = requests.Session()
        
        # 如果提供了 API Token
        if api_token:
            # Atlassian API Token (ATATT开头) 需要配合用户名使用 Basic Auth
            if api_token.startswith('ATATT'):
                if username:
                    self.session.auth = (username, api_token)
                else:
                    # 如果没有提供用户名，尝试从环境或使用token作为密码
                    # Bitbucket Cloud 支持使用邮箱+Atlassian token
                    print("警告: Atlassian API Token 通常需要配合用户名/邮箱使用")
                    self.session.auth = ('x-token-auth', api_token)
            # Repository/Workspace Access Token (bbs_开头) 使用 Bearer
            elif api_token.startswith('bbs_'):
                self.session.headers.update({
                    'Authorization': f'Bearer {api_token}'
                })
            # 其他格式的token，尝试 Bearer 方式
            else:
                self.session.headers.update({
                    'Authorization': f'Bearer {api_token}'
                })
        # 如果提供了用户名和密码，使用基本认证
        elif username and app_password:
            self.session.auth = (username, app_password)
        # 如果提供了用户名和App Password，使用基本认证（旧方式，已废弃）
        elif username and app_password:
            self.session.auth = (username, app_password)
    
    def get_pr_info(self, workspace: str, repo_slug: str, pr_id: int) -> Dict[str, Any]:
        """
        获取PR的基本信息
        
        Args:
            workspace: 工作空间名称（例如：your-workspace）
            repo_slug: 仓库名称（例如：your-repo）
            pr_id: PR编号
            
        Returns:
            包含PR信息的字典
        """
        url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/pullrequests/{pr_id}"
        
        try:
            response = self.session.get(url)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"获取PR信息失败: {e}")
            return {}
    
    def get_pr_commits(self, workspace: str, repo_slug: str, pr_id: int) -> List[Dict[str, Any]]:
        """
        获取PR中的所有提交
        
        Args:
            workspace: 工作空间名称
            repo_slug: 仓库名称
            pr_id: PR编号
            
        Returns:
            包含所有提交信息的列表
        """
        url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/pullrequests/{pr_id}/commits"
        
        try:
            response = self.session.get(url)
            response.raise_for_status()
            data = response.json()
            return data.get('values', [])
        except requests.exceptions.RequestException as e:
            print(f"获取PR提交信息失败: {e}")
            return []
    
    def get_pr_diff(self, workspace: str, repo_slug: str, pr_id: int) -> str:
        """
        获取PR的diff信息
        
        Args:
            workspace: 工作空间名称
            repo_slug: 仓库名称
            pr_id: PR编号
            
        Returns:
            diff文本内容
        """
        url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/pullrequests/{pr_id}/diff"
        
        try:
            response = self.session.get(url)
            response.raise_for_status()
            return response.text
        except requests.exceptions.RequestException as e:
            print(f"获取PR diff失败: {e}")
            return ""
    
    def get_pr_comments(self, workspace: str, repo_slug: str, pr_id: int) -> List[Dict[str, Any]]:
        """
        获取PR的所有评论
        
        Args:
            workspace: 工作空间名称
            repo_slug: 仓库名称
            pr_id: PR编号
            
        Returns:
            包含所有评论的列表
        """
        url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/pullrequests/{pr_id}/comments"
        
        try:
            response = self.session.get(url)
            response.raise_for_status()
            data = response.json()
            return data.get('values', [])
        except requests.exceptions.RequestException as e:
            print(f"获取PR评论失败: {e}")
            return []
    
    def get_pr_activity(self, workspace: str, repo_slug: str, pr_id: int) -> List[Dict[str, Any]]:
        """
        获取PR的活动记录
        
        Args:
            workspace: 工作空间名称
            repo_slug: 仓库名称
            pr_id: PR编号
            
        Returns:
            包含所有活动记录的列表
        """
        url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/pullrequests/{pr_id}/activity"
        
        try:
            response = self.session.get(url)
            response.raise_for_status()
            data = response.json()
            return data.get('values', [])
        except requests.exceptions.RequestException as e:
            print(f"获取PR活动记录失败: {e}")
            return []
    
    def print_pr_summary(self, pr_info: Dict[str, Any]):
        """
        打印PR摘要信息
        
        Args:
            pr_info: PR信息字典
        """
        if not pr_info:
            print("无PR信息可显示")
            return
        
        print("=" * 80)
        print(f"PR #{pr_info.get('id')}: {pr_info.get('title')}")
        print("=" * 80)
        print(f"状态: {pr_info.get('state')}")
        print(f"作者: {pr_info.get('author', {}).get('display_name')}")
        print(f"创建时间: {pr_info.get('created_on')}")
        print(f"更新时间: {pr_info.get('updated_on')}")
        print(f"源分支: {pr_info.get('source', {}).get('branch', {}).get('name')}")
        print(f"目标分支: {pr_info.get('destination', {}).get('branch', {}).get('name')}")
        print(f"\n描述:")
        print(pr_info.get('description', '无描述'))
        print("=" * 80)


def main():
    """主函数 - 从命令行参数解析PR URL"""
    import sys
    import re
    import os
    
    # 从命令行获取PR URL
    if len(sys.argv) < 2:
        print("用法: python bitbucket_pr_reader.py <PR_URL>")
        print("示例: python bitbucket_pr_reader.py https://bitbucket.org/your-workspace/your-repo/pull-requests/42")
        print("\n认证信息:")
        print("  请设置环境变量 BITBUCKET_API_TOKEN 和 BITBUCKET_USERNAME")
        print("  或使用公开仓库（无需认证）")
        sys.exit(1)
    
    pr_url = sys.argv[1]
    
    # 解析URL
    # 支持格式: https://bitbucket.org/{workspace}/{repo_slug}/pull-requests/{pr_id}
    pattern = r'https://bitbucket\.org/([^/]+)/([^/]+)/pull-requests/(\d+)'
    match = re.match(pattern, pr_url)
    
    if not match:
        print(f"错误: 无法解析PR URL: {pr_url}")
        print("URL格式应为: https://bitbucket.org/workspace/repo/pull-requests/PR_ID")
        sys.exit(1)
    
    workspace = match.group(1)
    repo_slug = match.group(2)
    pr_id = int(match.group(3))
    
    # 从环境变量读取认证信息
    api_token = os.environ.get('BITBUCKET_API_TOKEN')
    username = os.environ.get('BITBUCKET_USERNAME')
    
    # 创建PR读取器实例
    reader = BitbucketPRReader(api_token=api_token, username=username)
    
    # 如果仓库是公开的，可以不使用认证：
    # reader = BitbucketPRReader()
    
    print("正在获取PR信息...\n")
    
    # 1. 获取PR基本信息
    pr_info = reader.get_pr_info(workspace, repo_slug, pr_id)
    if pr_info:
        reader.print_pr_summary(pr_info)
    
    # 2. 获取PR中的提交
    print("\n正在获取提交信息...")
    commits = reader.get_pr_commits(workspace, repo_slug, pr_id)
    print(f"共有 {len(commits)} 个提交")
    for i, commit in enumerate(commits[:5], 1):  # 只显示前5个
        print(f"  {i}. {commit.get('hash', '')[:7]} - {commit.get('message', '').split(chr(10))[0]}")
    
    # 3. 获取PR的diff（代码变更）
    print("\n正在获取代码变更diff...")
    diff = reader.get_pr_diff(workspace, repo_slug, pr_id)
    if diff:
        print(f"Diff大小: {len(diff)} 字符")
        # 可以保存到文件
        # with open(f"pr_{pr_id}_diff.txt", "w", encoding="utf-8") as f:
        #     f.write(diff)
        # print(f"Diff已保存到 pr_{pr_id}_diff.txt")
    
    # 4. 获取评论
    print("\n正在获取评论...")
    comments = reader.get_pr_comments(workspace, repo_slug, pr_id)
    print(f"共有 {len(comments)} 条评论")
    for i, comment in enumerate(comments[:3], 1):  # 只显示前3条
        content = comment.get('content', {}).get('raw', '')
        author = comment.get('user', {}).get('display_name', 'Unknown')
        print(f"  {i}. {author}: {content[:50]}...")
    
    # 5. 获取活动记录
    print("\n正在获取活动记录...")
    activities = reader.get_pr_activity(workspace, repo_slug, pr_id)
    print(f"共有 {len(activities)} 条活动记录")
    
    # 6. 将完整信息保存到JSON文件（可选）
    output_data = {
        "pr_info": pr_info,
        "commits": commits,
        "diff": diff,
        "comments": comments,
        "activities": activities
    }
    
    output_file = f"pr_{pr_id}_full_info.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(output_data, f, indent=2, ensure_ascii=False)
    
    print(f"\n✓ 完整信息已保存到 {output_file}")


if __name__ == "__main__":
    main()
