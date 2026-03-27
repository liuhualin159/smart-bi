# PDF 中文字体

导出看板/图表为 PDF 时，需要中文字体才能正确显示中文。

## 自动检测顺序

1. **Windows**：自动使用 `C:\Windows\Fonts\msyh.ttc`（微软雅黑）或 `simsun.ttc`（宋体）
2. **Linux**：检测 `/usr/share/fonts/` 下的 Noto CJK 或文泉驿字体
3. **Classpath**：将字体放入本目录（`src/main/resources/fonts/`）

## 手动添加字体（可选）

若系统无中文字体，可将以下任一字体复制到本目录并命名：

- `NotoSansCJKsc-Regular.ttf` - 从 [Noto CJK  releases](https://github.com/notofonts/noto-cjk/releases) 下载
- `simsun.ttf` - Windows 宋体
- `msyh.ttf` - Windows 微软雅黑

复制后重新编译部署即可。
