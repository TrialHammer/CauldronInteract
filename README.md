# CauldronInteract

CauldronInteract 为 [BreweryX](https://github.com/BreweryTeam/BreweryX) 提供自动化炼药锅与蒸馏流程：
发射器加水、投料、装瓶，以及通过漏斗向酿造台输入后自动启动 BreweryX 蒸馏。

## 功能

- 自动装瓶：使用玻璃瓶从 BreweryX 炼药锅中提取酒液。
- 自动投料：向装水的炼药锅投入 BreweryX 支持的原料，并按配置返还空容器。
- 自动加水：使用水桶补满空炼药锅或未满炼药锅。
- 漏斗蒸馏：酿造台通过漏斗收到原料后自动启动或继续 BreweryX 蒸馏。
- 发射器升级：可选要求发射器先由玩家用锄头升级。
- `/cauldroninteract reload|status`（别名 `/ci`）：热重载配置并查看功能开关。

蒸馏配方、时间和产物提取限制仍由 BreweryX 管理。

## 运行环境

- Paper 26.3 或 Paper 1.21.11（兼容整个 Paper 1.21.x 系列）
- Java 21+
- 已安装并启用的 BreweryX

## 构建

项目由 Maven 管理，并提供双目标构建脚本：

```bash
bash scripts/build.sh                     # Paper 26.3，默认
PAPER_API_VERSION=1.21.11 bash scripts/build.sh
```

产物写入 `dist/CauldronInteract-1.5.0-<Paper版本>.jar`，例如
`dist/CauldronInteract-1.5.0-26.3.jar` 和
`dist/CauldronInteract-1.5.0-1.21.11.jar`。1.21.x 产物声明
`api-version: 1.21`，同一个 jar 可加载到 1.21.1 至 1.21.11；26.3 产物声明
`api-version: 26.3`。

直接执行 `mvn clean package` 仍会构建默认的 Paper 26.3 版本。测试：

```bash
mvn test
```

## 配置

所有可调项位于 `plugins/CauldronInteract/config.yml`，玩家可见文本位于 `messages.yml`。
`config.yml` 覆盖：

- `features.*`：加水、装瓶、投料、发射器升级、蒸馏、检测周期。
- `behaviour.*`：输入延迟、空容器返还、补水方式、瓶/桶材料匹配。
- `effects.*`：音效、粒子及音量/音调。
- `messages.yml`：命令、状态、升级提示等所有文本。

## 从上游/旧版迁移

旧配置键 `enable-dispenser-upgrade` 仍可读取；新配置请使用
`features.dispenser-upgrade.enabled`。旧版 Gradle 项目已迁移到 Maven，Gradle
构建文件不再参与发布流程。
