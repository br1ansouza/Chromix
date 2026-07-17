# Chromix

Jogo de puzzle **Ball Sort** para Android com identidade gaúcha: organize bolinhas coloridas em frascos até cada frasco conter uma única cor.

- 100% offline, sem anúncios, sem contas, sem música de fundo
- Fases infinitas geradas proceduralmente, sempre solucionáveis, com dificuldade crescente
- Paleta "pôr-do-sol no pampa", detalhes do RS (bandeira, exclamações tchê)
- Feedback tátil (vibração) e sonoro (efeitos curtos), cada um com toggle próprio
- Kotlin + Jetpack Compose, arquitetura MVVM

## Como jogar

Toque num frasco para levantar a bolinha do topo (click + vibração sutil), depois toque no destino. O movimento vale se o destino estiver vazio ou com bolinha da mesma cor no topo (e não estiver cheio) — a bolinha voa em arco até o lugar. Movimento inválido dá shake no frasco. Vença quando todo frasco estiver vazio ou completo com uma cor só: "Bah, tri massa!".

- **Undo** e **reset** ilimitados, sem custo
- **Toggles no HUD**: som e vibração, salvos entre sessões
- **Tela de níveis**: progressão linear, concluídos com check, nível atual destacado
- **Dificuldade**: 4→12 cores (+1 a cada 3 níveis), tubos vazios 2→1 no nível 15, capacidade 4→6 a partir do 21

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Kotlin 2.0 |
| UI | Jetpack Compose (Compose Animation, navigation-compose) |
| Arquitetura | MVVM (ViewModel + StateFlow) |
| Persistência | DataStore (Preferences) |
| Haptics | `VibrationEffect` (API 26+) com fallback legado |
| Som | `SoundPool` (efeitos curtos, baixa latência) |
| Build | Gradle 8.11 (Kotlin DSL), AGP 8.7, minSdk 24, targetSdk 35 |

## Estrutura

```
app/src/main/java/com/br1ansouza/chromix/
├── data/        # GamePreferences (DataStore: nível, recorde, som, vibração)
├── domain/      # Modelos, regras e gerador de fases (puro Kotlin, testável na JVM)
├── ui/
│   ├── home/    # Tela inicial: logo, listras do RS, botão Iniciar
│   ├── game/    # Tabuleiro Canvas, animações (voo Bézier, shake, pulso, lift),
│   │            # HUD, card de vitória, paleta com marcadores p/ daltonismo
│   ├── levels/  # Grade de níveis (9999+ exibidos, geração sob demanda)
│   ├── haptics/ # GameHaptics (seleção 10ms, válido 12ms, erro 30ms,
│   │            # tubo completo 40ms, vitória em waveform duplo)
│   ├── sound/   # GameSounds (click de seleção, chime de vitória)
│   └── theme/   # Tema dark fixo com acentos da paleta RS
└── viewmodel/   # GameViewModel (estado, undo, eventos)
```

O gerador cria a fase "de trás pra frente" a partir do estado resolvido, com movimentos inversos — cada fase é solucionável por construção e determinística pelo número do nível. Testes unitários reexecutam a solução gravada das fases 1–60.

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
adb install -r app/build/outputs/apk/debug/app-debug.apk

# testes unitários (regras + solvabilidade do gerador)
./gradlew :app:testDebugUnitTest
```

> WSL2 não enxerga USB: use o `adb.exe` do Android SDK do Windows (ex.:
> `/mnt/c/Users/<user>/AppData/Local/Android/Sdk/platform-tools/adb.exe`) apontando
> para o APK com caminho Windows.

### APK de release

```bash
./gradlew :app:assembleRelease
```

O APK sai minificado (R8) porém **sem assinatura**. Para instalar, assine com sua própria keystore (Android Studio: *Build > Generate Signed App Bundle / APK*) ou use o debug APK, que já resolve para uso pessoal.

## Créditos

- Efeitos sonoros: [Pixabay](https://pixabay.com/sound-effects/) (royalty-free)
- Logo: arte própria com temática gaúcha
