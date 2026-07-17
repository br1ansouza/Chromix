# Chromix — Contexto do projeto

Jogo Android nativo de puzzle "Ball Sort": mover bolinhas coloridas entre frascos até cada frasco ter uma cor só. App pessoal, local, offline. Sem backend, sem anúncios, sem contas, sem música — apenas haptics.

## Regras de workflow (Git)

- **Fluxo de features** (mesmo padrão do projeto TrackRide):
  1. Criar branch `feature/<nome-kebab-case>` a partir da `main` atualizada.
  2. Commits pequenos e separados por mudança lógica dentro da feature.
  3. Ao finalizar a feature: abrir PR para a `main` e fazer merge.
  4. Próxima feature sempre parte da `main` pós-merge.
- **Commits**: Conventional Commits em minúsculas, em inglês.
  - Tipos usados: `feat:`, `fix:`, `docs:`, `chore:`, `ci:`, `style:`, `refactor:`, `test:`.
  - Assunto ≤ 50 caracteres, imperativo. Corpo só quando o "porquê" não for óbvio.
- **PRs**: Claude tem autonomia total para criar e mergear PRs neste repositório.
- Remote: `https://github.com/br1ansouza/Chromix.git`.

## Regras de produto (resumo da especificação)

- **Mecânica**: mover bolinha do topo de um frasco para outro apenas se o destino estiver vazio ou tiver bolinha do topo da mesma cor, e não estiver cheio. Vitória quando todo frasco está vazio ou cheio de uma cor só.
- **Fases infinitas**: geração procedural reversa (embaralhar a partir do estado resolvido com movimentos válidos) — solucionável por construção. Determinística: `Random(seed = levelNumber)`.
- **Curva de dificuldade**: `colorCount = min(4 + levelNumber/3, 12)`; `emptyTubes = 2` (< nível 15) senão `1`; capacidade 4, podendo variar 4–6 em níveis altos; `shuffleMoves = 40 + levelNumber * 3` (teto 300). Evitar fases triviais.
- **Telas**: três — inicial (logo + Iniciar), jogo e seleção de níveis (grid sob demanda, progressão linear). Sem tela de configurações: toggles de som e vibração no HUD.
- **UX**: animação de voo em arco (~200–280ms, FastOutSlowInEasing), shake em movimento inválido, pulso em tubo completo, overlay de vitória com avanço automático, transições ~200ms. Geração de fase < 50ms, sem loading.
- **Haptics**: válido 10–15ms, inválido 30ms, tubo completo 40ms, vitória waveform duplo. Respeitar toggle salvo.
- **Som**: efeitos curtos via SoundPool (click seleção WAV, chime vitória mp3), toggle próprio. Sem música.
- **Persistência**: DataStore Preferences — `currentLevel`, `bestLevelReached`, `vibrationEnabled`, `soundEnabled`.
- **Visual**: dark theme fixo (fundo `#121216`), paleta RS "pôr-do-sol no pampa" (12 cores, ver `BallPalette.kt`); marcador geométrico por cor para acessibilidade (daltonismo). Acentos: primary `#2FB8AC`, secondary `#F6B149`. Detalhes RS: listras da bandeira (home e card de vitória), exclamações gaúchas na vitória.

## Ambiente local (WSL)

- Android SDK: `/home/brian/android-sdk` (platforms 34 e 35, build-tools 34/35). `local.properties` já aponta pra ele (não versionado).
- Java 21 (`/usr/bin/java`), Gradle via wrapper 8.11.1 (`./gradlew`), sem gradle global.
- Build: `./gradlew :app:assembleDebug` | Testes: `./gradlew :app:testDebugUnitTest`.
- `gh` autenticado como `br1ansouza`. Identidade git configurada localmente no repo.
- Logo original do jogo (1024x1024, bordas transparentes): cache de imagem da sessão; mipmaps já gerados em `app/src/main/res/mipmap-*`; cópia 512px em `drawable-nodpi/logo_chromix.png`.
- **Instalar no celular (S21 FE)**: WSL2 não vê USB — usar adb do Windows: `/mnt/c/Users/brian/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r 'C:\Users\brian\Desktop\Chromix.apk'`. Saída do adb.exe tem CRLF (`tr -d '\r'` antes de comparar). APK também é copiado pra área de trabalho do Windows a cada release de teste.
- ffmpeg estático no scratchpad da sessão (se sumir, baixar de johnvansickle.com) — usado pra converter áudio; SoundPool rejeita mp3 muito curto (erro -1010), converter pra WAV PCM.

## Status (2026-07-17)

Features concluídas (todas mergeadas na main via PR):
1. PR #1 `feature/project-setup` — scaffold Gradle/Compose, minSdk 24, target 35
2. PR #2 `feature/game-domain` — modelos, regras, gerador procedural (movimentos inversos + solução gravada; ver nota abaixo)
3. PR #3 `feature/game-ui` — GameViewModel, tabuleiro Canvas, voo Bézier, shake, pulso, HUD, overlay vitória
4. PR #4 `feature/app-icon` — logo do usuário como launcher icon (mipmaps + adaptive)
5. PR #5 `feature/haptics` — GameHaptics + toggle no HUD
6. PR #6 `feature/persistence` — DataStore (currentLevel, bestLevelReached, vibrationEnabled)
7. PR #7 `feature/level-select` — LevelsScreen + navigation-compose, progressão linear
8. PR #8 `feature/readme-docs` — README de build
9. PR #9 `fix/review-fixes` — coords do voo estáveis; grade não reseta nível atual
10. PR #10 `fix/flight-animation` — voo derivado na composição (sem frame de teleporte); lift animado
11. PR #11 `feature/home-screen` — tela inicial (logo, Iniciar), fundo global `#121216`, rota `home`
12. PR #12/#15 `fix/app-icon`/`fix/icon-fit` — ícone: arte completa na safe zone (70%), fundo escuro neutro
13. PR #13 `feature/levels-grid` — grade exibe 9999 níveis
14. PR #14 `fix/reset-transition` — reset com scale+fade
15. PR #16 `feature/rs-theme` — paleta RS nas bolinhas/tema, listras da bandeira, exclamações gaúchas
16. PR #17 `feature/sound-effects` — SoundPool + toggle som + card de vitória estilizado
17. PR #18 `fix/selection-sound` — click em WAV (mp3 48ms não decodificava), vibração na seleção

App validado no S21 FE do usuário (som, haptics, animações, persistência OK).

## Backlog (pedido pelo usuário em 2026-07-17, fazer na próxima sessão)

1. **Frascos maiores** — muita borda sobrando, principalmente no S25 dele: subir o teto de `ballSize` (hoje 52dp) e reduzir margens no `GameBoard` (rever `widthBased`/`heightBased`).
2. **Som ao desselecionar** — o click de seleção deve tocar também ao desselecionar a bolinha (tap no mesmo tubo).
3. **GIF do mate no card de vitória** — https://tenor.com/pt-BR/view/mate-matecito-mate-ciafba-mates-mate-agro-gif-6758900996126760062 (postid 6758900996126760062, aspect 0.66). Pequeno, como detalhe: sobreposto ao botão "Próximo nível", canto inferior direito, na diagonal. Baixar o arquivo real do media.tenor.com e embutir offline (coil-gif ou WebP animado em drawable).
4. **Movimento em grupo** — mover todas as bolinhas consecutivas de mesma cor do topo de uma vez, quantas couberem no destino (evita repetir 3x o mesmo movimento). Afeta `GameRules.applyMove` (retornar quantidade movida), animação de voo (bolinhas em fila), haptics/som (definir: 1 tick por grupo). Undo já cobre (snapshot de tubos). Solvabilidade preservada — multi-move é sequência de moves válidos.

**Nota do gerador**: a spec pedia embaralhar com movimentos válidos normais, mas isso mantém tubos monocromáticos (fase trivial). Implementado com movimentos inversos (remove bolinha sobre mesma cor ou do fundo, solta em tubo não cheio) — solucionável por construção; teste reexecuta a solução gravada (fases 1–60).

## Regras técnicas

- Kotlin nativo + Jetpack Compose, MVVM (ViewModel + StateFlow). minSdk 24, target SDK estável mais recente. Gradle Kotlin DSL.
- Pacotes: `data/`, `domain/` (regras e geração — puro Kotlin, testável sem Android), `ui/`, `viewmodel/`.
- Testes unitários obrigatórios para: validação de movimento, geração de fase (sempre solucionável), verificação de vitória.
- Zero dependências de rede, ads, analytics ou crash reporting de terceiros.
- Cold start rápido, sem splash pesada.
