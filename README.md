# Chromix

Jogo de puzzle **Ball Sort** para Android: organize bolinhas coloridas em frascos até cada frasco conter uma única cor.

- 100% offline, sem anúncios, sem contas, sem música — apenas haptics (vibração)
- Fases infinitas geradas proceduralmente, sempre solucionáveis, com dificuldade crescente
- Kotlin + Jetpack Compose, arquitetura MVVM

## Como jogar

Toque num frasco para levantar a bolinha do topo, depois toque no destino. O movimento só vale se o destino estiver vazio ou com bolinha da mesma cor no topo (e não estiver cheio). Vença quando todo frasco estiver vazio ou completo com uma cor só. Undo e reinício ilimitados; toggle de vibração no HUD; tela de níveis com progressão linear.

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Kotlin 2.0 |
| UI | Jetpack Compose (Compose Animation, navigation-compose) |
| Arquitetura | MVVM (ViewModel + StateFlow) |
| Persistência | DataStore (Preferences) |
| Haptics | `VibrationEffect` (API 26+) com fallback legado |
| Build | Gradle 8.11 (Kotlin DSL), AGP 8.7, minSdk 24, targetSdk 35 |

## Estrutura

```
app/src/main/java/com/br1ansouza/chromix/
├── data/        # GamePreferences (DataStore)
├── domain/      # Modelos, regras e gerador de fases (puro Kotlin, testável na JVM)
├── ui/
│   ├── game/    # Tela de jogo: tabuleiro Canvas, animações, HUD, overlay de vitória
│   ├── levels/  # Grade de níveis
│   ├── haptics/ # GameHaptics
│   └── theme/   # Tema dark fixo
└── viewmodel/   # GameViewModel
```

O gerador cria a fase "de trás pra frente" a partir do estado resolvido, com movimentos inversos — cada fase é solucionável por construção e determinística pelo número do nível.

## Build

Pré-requisitos: JDK 17+ e Android SDK (platform 35). Defina o caminho do SDK em `local.properties`:

```properties
sdk.dir=/caminho/para/android-sdk
```

Ou abra o projeto no Android Studio (que gera esse arquivo sozinho) e rode pelo botão Run.

Pela linha de comando:

```bash
# APK de debug (instalável direto, assinado com a chave de debug)
./gradlew :app:assembleDebug
# saída: app/build/outputs/apk/debug/app-debug.apk

# instalar num aparelho com depuração USB ligada
adb install app/build/outputs/apk/debug/app-debug.apk

# testes unitários (regras + solvabilidade do gerador)
./gradlew :app:testDebugUnitTest
```

### APK de release

```bash
./gradlew :app:assembleRelease
```

O APK sai minificado (R8) porém **sem assinatura**. Para instalar, assine com sua própria keystore (Android Studio: *Build > Generate Signed App Bundle / APK*) ou use o debug APK, que já resolve para uso pessoal.
