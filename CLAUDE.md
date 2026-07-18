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

- **Mecânica**: mover bolinha do topo de um frasco para outro apenas se o destino estiver vazio ou tiver bolinha do topo da mesma cor, e não estiver cheio. Frasco completo (cheio de uma cor só) fica travado — não pode mais ser origem. Vitória quando todo frasco está vazio ou cheio de uma cor só.
- **Fases infinitas**: distribuição totalmente aleatória das bolinhas, validada pelo solver de domínio (`Solver.kt`, mesma semântica de movimento em grupo do jogador); tabuleiro insolúvel ou trivial é rejeitado e regenerado. Determinística: `Random(levelNumber * 1000 + attempt)`. Beco sem saída em jogo dispara aviso no HUD (undo/reset).
- **Curva de dificuldade**: `colorCount = min(4 + levelNumber/3, 12)`; `emptyTubes = 2` sempre (1 vazio + tabuleiro misturado = quase sempre impossível, medido); capacidade 4 (<21), 4–5 (<41), 5–6 (41+). Anti-trivialidade: sem tubo pré-completo, sem fundo monocromático de 3+, transições de cor ≥ 60% do máximo.
- **Telas**: três — inicial (logo + Iniciar), jogo e seleção de níveis (grid sob demanda, progressão linear). Sem tela de configurações: toggles de som e vibração no HUD.
- **UX**: animação de voo em arco (~200–280ms, FastOutSlowInEasing), shake em movimento inválido, pulso em tubo completo, overlay de vitória com avanço automático, transições ~200ms. Geração de fase < 50ms, sem loading.
- **Haptics**: válido 10–15ms, inválido 30ms, tubo completo 40ms, vitória waveform duplo. Respeitar toggle salvo.
- **Som**: efeitos curtos via SoundPool (click seleção WAV, chime vitória mp3), toggle próprio. Sem música.
- **Persistência**: DataStore Preferences — `currentLevel`, `bestLevelReached`, `vibrationEnabled`, `soundEnabled`.
- **Visual**: dark theme fixo (fundo `#121216`), paleta RS "pôr-do-sol no pampa" (12 cores, ver `BallPalette.kt`); marcador geométrico por cor para acessibilidade (daltonismo). Acentos: primary `#2FB8AC`, secondary `#F6B149`. Detalhes RS: listras da bandeira (home e card de vitória), exclamações gaúchas na vitória.

## Regras técnicas

- Kotlin nativo + Jetpack Compose, MVVM (ViewModel + StateFlow). minSdk 24, target SDK estável mais recente. Gradle Kotlin DSL.
- Pacotes: `data/`, `domain/` (regras e geração — puro Kotlin, testável sem Android), `ui/`, `viewmodel/`.
- Testes unitários obrigatórios para: validação de movimento, geração de fase (sempre solucionável), verificação de vitória.
- Zero dependências de rede, ads, analytics ou crash reporting de terceiros.
- Cold start rápido, sem splash pesada.
