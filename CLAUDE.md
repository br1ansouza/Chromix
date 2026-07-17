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
- **Telas**: apenas duas — jogo (abre direto no nível em progresso) e seleção de níveis (grid sob demanda, progressão linear). Sem tela de configurações: toggle de vibração no HUD.
- **UX**: animação de voo em arco (~200–280ms, FastOutSlowInEasing), shake em movimento inválido, pulso em tubo completo, overlay de vitória com avanço automático, transições ~200ms. Geração de fase < 50ms, sem loading.
- **Haptics**: válido 10–15ms, inválido 30ms, tubo completo 40ms, vitória waveform duplo. Respeitar toggle salvo.
- **Persistência**: DataStore Preferences — `currentLevel`, `bestLevelReached`, `vibrationEnabled`.
- **Visual**: dark theme fixo (fundo preto), cores vibrantes; padrão/textura por cor para acessibilidade (daltonismo).

## Regras técnicas

- Kotlin nativo + Jetpack Compose, MVVM (ViewModel + StateFlow). minSdk 24, target SDK estável mais recente. Gradle Kotlin DSL.
- Pacotes: `data/`, `domain/` (regras e geração — puro Kotlin, testável sem Android), `ui/`, `viewmodel/`.
- Testes unitários obrigatórios para: validação de movimento, geração de fase (sempre solucionável), verificação de vitória.
- Zero dependências de rede, ads, analytics ou crash reporting de terceiros.
- Cold start rápido, sem splash pesada.
