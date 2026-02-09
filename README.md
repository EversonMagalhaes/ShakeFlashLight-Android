# 🔦 Shake Flashlight Android (v3.0)

Uma aplicação Android robusta e moderna projetada para ativar a lanterna do dispositivo através de um gesto de chacoalho (shake). Este projeto destaca-se por sua arquitetura resiliente, lidando com estados de segundo plano, inicialização pós-boot e filtragem inteligente de sensores.

## 🚀 Evolução Incremental
O grande diferencial deste repositório é a sua **documentação histórica**. O desenvolvimento foi dividido em 17 etapas críticas, cada uma isolada em sua própria branch, permitindo visualizar a evolução desde o "Hello World" até uma aplicação de nível de produção.

### 🛤️ Mapa das Branches
* **`step-01` ao `05`**: Setup do projeto, lógica inicial do `ShakeDetector` e refinamento de UI básica.
* **`step-06` ao `10`**: Persistência com `SharedPreferences`, implementação de `Foreground Service` e modernização para as SDKs mais recentes.
* **`step-11` ao `15`**: Identidade visual, animações fluidas, suporte a `BroadcastReceiver` para Auto-start no boot e lançamento da v3.0.
* **`step-16` e `17`**: Refinamento de UX com níveis de sensibilidade discretos e filtros contra ruídos de sensores no Android 13.

## 🛠️ Funcionalidades Técnicas
- **Detecção Inteligente**: Algoritmo que exige movimentos rítmicos para evitar acionamentos acidentais.
- **Modo Background**: Utiliza *Foreground Services* para garantir que o app funcione mesmo com a tela desligada.
- **Níveis de Intensidade**: Escala de sensibilidade personalizada (Sensível, Padrão, Firme e Intenso).
- **Auto-Boot**: Inicialização automática do serviço ao ligar o aparelho.
- **Estabilidade**: Filtro contra "picos" de sensores comuns em tablets e versões recentes do Android.

## 📱 Tecnologias e Conceitos
- **Linguagem**: Kotlin
- **Android SDK**: Sensores (Acelerômetro), Camera2 API (Flashlight).
- **Componentes**: `Service`, `BroadcastReceiver`, `NotificationManager`.
- **UI/UX**: Material Design, Custom SeekBars e Animações de