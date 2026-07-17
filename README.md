# Chromix

Jogo de puzzle **Ball Sort** para Android: organize bolinhas coloridas em frascos até cada frasco conter uma única cor.

- 100% offline, sem anúncios, sem contas, sem música — apenas haptics (vibração)
- Fases infinitas geradas proceduralmente, sempre solucionáveis, com dificuldade crescente
- Kotlin + Jetpack Compose, arquitetura MVVM

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Kotlin |
| UI | Jetpack Compose (Compose Animation) |
| Arquitetura | MVVM (ViewModel + StateFlow) |
| Persistência | DataStore (Preferences) |
| Haptics | `VibrationEffect` (API 26+) com fallback |
| Build | Gradle (Kotlin DSL), minSdk 24 |

## Build

Instruções detalhadas de build e geração de APK serão adicionadas conforme o projeto evolui.

```bash
git clone https://github.com/br1ansouza/Chromix.git
```

Abra o projeto no Android Studio e sincronize o Gradle.
