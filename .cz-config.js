module.exports = {
  types: [
    { value: 'feat', name: 'feat:     新功能' },
    { value: 'fix', name: 'fix:      修复bug' },
    { value: 'docs', name: 'docs:     文档变更' },
    { value: 'style', name: 'style:    代码格式调整' },
    { value: 'refactor', name: 'refactor: 重构代码' },
    { value: 'test', name: 'test:     测试相关' },
    { value: 'chore', name: 'chore:    构建/工具变更' },
    { value: 'revert', name: 'revert:   回退提交' }
  ],

  scopes: [
    { name: 'api' },
    { name: 'ui' },
    { name: 'auth' },
    { name: 'db' },
    { name: 'config' },
    { name: 'deps' }
  ],

  messages: {
    type: '选择提交类型:',
    scope: '选择影响范围 (可选):',
    subject: '简短描述:\n',
    body: '详细描述 (可选):\n',
    breaking: '破坏性变更说明 (可选):\n',
    footer: '关联Issue (可选，如 "Closes #123"):\n',
    confirmCommit: '确认提交?'
  }
};