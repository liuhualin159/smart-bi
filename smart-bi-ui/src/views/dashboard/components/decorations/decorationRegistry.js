import TitleBar from './TitleBar.vue'
import BorderBox from './BorderBox.vue'
import DividerLine from './DividerLine.vue'

export const decorationComponents = {
  'title-bar': TitleBar,
  'border-box': BorderBox,
  'divider-line': DividerLine,
}

export const decorationDefaults = {
  'title-bar': {
    title: '标题',
    fontSize: 18,
    fontColor: '#ffffff',
    backgroundColor: 'linear-gradient(90deg, #568aea 0%, transparent 100%)',
    borderColor: '#568aea',
  },
  'border-box': {
    borderStyle: 'tech-border-1',
    borderColor: '#568aea',
    borderWidth: 2,
    animationEnabled: true,
  },
  'divider-line': {
    lineStyle: 'solid',
    lineColor: '#568aea',
    lineWidth: 1,
    orientation: 'horizontal',
  },
}

export const decorationOptions = [
  { type: 'title-bar', label: '标题栏', icon: 'EditPen' },
  { type: 'border-box', label: '边框装饰', icon: 'FullScreen' },
  { type: 'divider-line', label: '分隔线', icon: 'SemiSelect' },
]
