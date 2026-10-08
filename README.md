# CauldronInteract

CauldronInteract 是一个面向 [BreweryX](https://github.com/BreweryTeam/BreweryX) 的 Minecraft 服务端插件，支持使用发射器为炼药锅加水、投料和装瓶，以及通过漏斗自动启动蒸馏。

## 功能

- 自动装瓶：使用玻璃瓶从 BreweryX 炼药锅中提取酒液。
- 自动投料：向装水的炼药锅投入 BreweryX 支持的原料，使用后返还相应的空容器。
- 自动加水：使用水桶将空炼药锅或未满的水炼药锅补满。
- 漏斗蒸馏：酿造台通过漏斗收到物品后，自动检查并启动 BreweryX 蒸馏流程。后续输入不会重置蒸馏计时。
- 发射器升级：可通过配置要求发射器先升级，再使用上述炼药锅交互功能。

蒸馏配方、时间和产物提取限制由 BreweryX 管理。

## 运行环境

- 提供兼容 Spigot API 的 Minecraft 服务端。当前项目使用 `1.21.11-R0.2-SNAPSHOT` 编译。
- 已安装并启用的 BreweryX 插件。
- 满足服务端要求的 Java 版本。本插件的编译目标为 Java 17。

## 安装

1. 按照[构建说明](#构建)生成插件 JAR。
2. 将 `build/libs/CauldronInteract-1.4.1-RELEASE.jar` 放入已安装 BreweryX 的服务器的 `plugins/` 目录。
3. 重启服务器，首次启动会生成 `plugins/CauldronInteract/config.yml`。
4. 按需修改配置，再重启服务器使其生效。

## 使用

将发射器朝向炼药锅，放入水桶、酿造原料或玻璃瓶，再使用红石信号触发。

- 加水时，炼药锅会补满至 3 格，空桶掉落在发射器下方；炼药锅已满时，水桶留在发射器中。补水会清除炼药锅中已有的 BreweryX 酒液数据。
- 装瓶仅适用于 BreweryX 已记录的炼药锅。酒液和投料后返还的空容器放回发射器，空间不足时掉落在其下方。
- 蒸馏时，在普通酿造台的原料槽中放入荧石粉，再通过漏斗向瓶槽输入饮品。插件每 0.5 秒检查已加载的酿造台，具备可蒸馏的酒液和荧石粉后自动启动 BreweryX 蒸馏，保留原有动画和计时。

本版本专注于 BreweryX 自动酿造，不包含上游的智能发射器和自动取岩浆功能。

BreweryX 版本不兼容时，相关操作会跳过，并在服务器日志中记录警告。

## 配置

```yaml
# 是否要求发射器先升级
enable-dispenser-upgrade: false
```

默认值为 `false`，所有发射器均可使用。设置为 `true` 后，玩家需要手持任意锄头，潜行并右键发射器进行升级。升级会播放音效和粒子，非创造模式下消耗 1 点锄头耐久。关闭再开启此选项后，已有的升级标记仍然有效。

## 构建

构建需要 JDK 25。项目提供 Gradle Wrapper，首次构建会下载 Gradle 和依赖。

在项目目录中执行以下命令。

Linux / macOS：

```bash
bash gradlew clean build
```

Windows：

```bat
gradlew.bat clean build
```

构建产物位于 `build/libs/`。JAR 中的 `META-INF/` 目录包含许可证和 README。

## 测试

`build` 任务包含测试，也可以单独运行：

```bash
# Linux / macOS
bash gradlew test
```

```bat
:: Windows
gradlew.bat test
```

测试报告位于 `build/reports/tests/test/index.html`。

## 项目来源

本项目基于 [VoxCrafterLP/CauldronInteract](https://github.com/VoxCrafterLP/CauldronInteract) 修改，原作者为 VoxCrafter_LP。

修改版说明（2026-10-08）：调整炼药锅交互逻辑，集成 BreweryX 自动投料、酒液装瓶和漏斗蒸馏，并精简原版功能及配置。

## 贡献者

- [VoxCrafter_LP](https://github.com/VoxCrafterLP)：原项目作者。
- savagetricycle：贡献者。

## 许可证

本项目沿用上游的 GNU General Public License v3.0（GPL-3.0）许可证，完整条款见 [LICENSE](LICENSE)。
