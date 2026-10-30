# 影视多维高级筛选功能实现计划 (Year, Type, Region, Genre)

## 1. 核心需求与设计方案

### 现状分析
当前从影视源（MacCMS V10 / AppleCMS）获取数据时，默认请求 `ac=detail` 仅返回最新更新的前 20~30 部作品（第一页）。如果在本地客户端对这几十部最新影视进行过滤，将根本无法检索到历史经典电影（例如 1999 年上映的《黑客帝国》、《星际迷航》或其他年代久远的电影）。

### 解决方案
根据用户确认的选项：
1. **服务端多维参数直查 + 搜索回退策略**：
   - MacCMS V10 标准接口支持在 `ac=detail` 请求中携带 `t`（分类 ID）、`year`（年份，如 `1999`）、`area`（地区，如 `美国`）、`class`（类型标签，如 `科幻`）及分页 `pg` 参数。
   - 当用户在筛选面板中选定年份、地区、类别标签时，网络请求直接向源服务端发送对应参数，由源站数据库在全站数据中过滤检索出 1999 年美国的科幻电影。
   - **智能回退保障**：若目标源未启用参数索引导致返回 0 条结果，自动将筛选组合转换为关键词检索组合（如拼接 `wd=1999 科幻`）或发起多源聚合检索，确保不错漏内容。
2. **底部抽屉筛选弹窗 (Filter BottomSheet)**：
   - 影视页右上角或搜索栏旁增设「多维筛选」触发按钮，展示当前生效的过滤标签数量徽标。
   - 点击弹出 Material 3 `ModalBottomSheet`，分层级展示：
     - **主分类**：电影、电视剧、综艺、动漫、纪录片等。
     - **地区**：全部、中国大陆、中国香港、中国台湾、美国、韩国、日本、英国、法国、其他。
     - **剧情类型/风格**：全部、科幻、动作、喜剧、爱情、悬疑、恐怖、战争、犯罪、冒险、动画等。
     - **年份选择**：近几年快捷选择（2024、2023、2022、2021 等），以及**「自定义年份」精准输入入口**。
     - 底部操作栏：一键「重置」与「完成筛选 (应用)」。
3. **精准自定义年份弹窗 (Custom Year Dialog)**：
   - 点击「自定义年份」弹出数字输入框，用户可直接键入任意具体年份（例如 `1999`），支持快速校验（1900 ~ 当前年份），填入后立即应用到筛选条件中。
4. **主界面即时指示与一键清空**：
   - 筛选生效后，在影视页顶部展示已激活的筛选胶囊标签（如 `[1999年] [美国] [科幻] [✕]`），用户可一键删除单个筛选条件或清空恢复全部。

---

## 2. 详细改动模块

### A. 数据与服务层 (Data & Domain Layer)
- **文件**: `/app/src/main/java/com/yuivo9999/mybox123/domain/movie/NativeMovieSourceService.kt`
  - 扩展 `fetchMovies` 方法与底层 `executeCmsQuery`，接收多维过滤模型：
    - `typeId: String? = null`
    - `year: String? = null`
    - `area: String? = null`
    - `genre: String? = null` (映射为 `class`)
    - `page: Int = 1`
  - URL 构建加入安全 URL 编码：`&t={typeId}&year={year}&area={area}&class={genre}&pg={page}`。
  - 实现空结果回退机制：若服务端过滤返回空结果且包含年份/类型，尝试通过 `wd` 参数执行智能检索回退。

### B. 状态管理与视图模型 (ViewModel Layer)
- **文件**: `/app/src/main/java/com/yuivo9999/mybox123/ui/screens/MovieCatalogViewModel.kt`
  - 定义 `MovieFilterCriteria` 数据类：
    - `category: MovieCategoryOption?`
    - `year: String?`
    - `area: String?`
    - `genre: String?`
  - ViewModel 状态中维护：
    - `currentFilters: StateFlow<MovieFilterCriteria>`
    - `isFilterSheetVisible: Boolean`
    - `isYearInputDialogVisible: Boolean`
  - 增加操作方法：
    - `updateFilters(newCriteria: MovieFilterCriteria)`
    - `setCustomYear(year: String)`
    - `clearSingleFilter(type: FilterType)`
    - `resetAllFilters()`

### C. 界面与交互层 (Compose UI Layer)
- **新建/修改文件**:
  - `/app/src/main/java/com/yuivo9999/mybox123/ui/screens/MovieFilterBottomSheet.kt`（新创建）：
    - 采用 Material 3 沉浸式暗色设计，包含分类 Chip 组、地区 Chip 组、类型 Chip 组、年份 Chip 组。
    - 「自定义年份」Chip 样式高亮，点击唤起年份输入对话框。
  - `/app/src/main/java/com/yuivo9999/mybox123/ui/screens/CustomYearInputDialog.kt`（新创建）：
    - 规范数字键盘输入、年份有效性提示（如 1999）、确定与取消。
  - `/app/src/main/java/com/yuivo9999/mybox123/ui/screens/NativeMovieCatalogScreen.kt`：
    - 顶部操作栏加入「筛选」图标按钮（带激活红点/徽标）。
    - 列表顶部加入活动筛选标签展示区（如 `1999`、`美国`、`科幻` 等 Chips）。
    - 挂载 `MovieFilterBottomSheet` 与 `CustomYearInputDialog`。

---

## 3. 验证与测试步骤

1. **编译检查**：
   - 运行 `compile_applet` 确保 Kotlin 代码与 Compose 组件无语法或类型错误。
2. **URL 与参数生成测试**：
   - 验证选中 `1999` + `美国` + `科幻` + `电影` 时，请求 URL 正确构建为类似 `...&ac=detail&t=1&year=1999&area=%E7%BE%8E%E5%9B%BD&class=%E7%A7%91%E5%89%98&pg=1`。
3. **UI 流程测试**：
   - 打开影视界面 -> 点击筛选按钮弹出 BottomSheet -> 选择「电影」、「美国」、「科幻」 -> 点击「自定义年份」输入「1999」并确定 -> 点击「完成筛选」 -> 界面展示筛选结果与顶部筛选标签 -> 点击标签上的 ✕ 可快速移除单个条件。
