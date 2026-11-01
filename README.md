# MXQ Galaxy

Versão personalizada do [CleanYourTV](https://github.com/Gathin23/cleanyourtv), desenvolvida para o MXQ Pro de hugulinux. Aplicativo Android nativo em Java: `com.gathin.tvhome`, versão `3.0-galaxy`, versionCode 9.

Base original: commit `76a9312cd9ac378fc8698f31427805af32c6f965`. Este repositório guarda nossa personalização, sem incorporar automaticamente versões posteriores do original.

## Recursos personalizados

- Galaxy nativo em Canvas, inspirado no React Bits, limitado a 20 FPS e pausado fora da tela inicial.
- Papéis de parede sem distorção, seleção individual e rotação aleatória ou sequencial. Intervalos de 10 a 60 minutos ou personalizados entre 1 e 1440 minutos. Escolher imagens desativa o Galaxy.
- Iluminação ambiente com a cor do app selecionado, intensidade ajustada e suavização dos degradês para o MXQ.
- Clima com cidade configurável e ícones conforme as condições meteorológicas.
- Dock personalizada, ícones aprimorados e botões superiores circulares com legendas discretas apenas no foco.
- Configurações em dois painéis: Organização e Personalização, com miniatura do wallpaper e indicadores de estado.
- Botão para limpar processos de apps em segundo plano sem apagar dados. O Android pode reiniciar serviços; Android 14 e posteriores restringem a operação a processos do próprio app.
- BorderGlow adaptado para Canvas/Drawable: bordas roxa, rosa e azul na dock e nos botões, com destaque no foco do controle e sem animação contínua.

## Estrutura

- `launcher/`: projeto Android e Gradle Wrapper.
- `tools/`: utilitários herdados do CleanYourTV.
- `dist/MXQ-Galaxy-3.0-galaxy.apk`: APK compilado com a assinatura usada na nossa instalação.
- `examples/config.json`: exemplo de tema sem dados de conta ou IP do aparelho.
- `docs/menu.png`: captura do menu personalizado.

As preferências privadas escolhidas na TV, contas de outros apps e imagens pessoais de wallpaper não fazem parte do código. Para a rotação, coloque suas imagens em `Android/data/com.gathin.tvhome/files/wallpapers/` no armazenamento compartilhado da TV.

## Compilar

Requer JDK 17, Android SDK Platform 34 e Build Tools 34.0.0. Configure `JAVA_HOME` e `ANDROID_HOME` para seu computador. No diretório `launcher`, execute:

```powershell
.\gradlew.bat --no-daemon assembleRelease
```

O resultado fica em `launcher/app/build/outputs/apk/release/app-release.apk`. A primeira compilação pode baixar Gradle e dependências.

**Assinatura:** release usa a chave de debug do ambiente. Outro computador normalmente criará outra chave. Atualizar a instalação existente preservando dados exige a chave original, mantida fora do GitHub. Nunca adicione arquivos `.keystore` ou `.jks` ao repositório. O APK de `dist/` já tem a assinatura usada no nosso aparelho.

## Instalar ou atualizar

Com ADB conectado à TV, a partir da raiz do projeto:

```powershell
adb install -r dist/MXQ-Galaxy-3.0-galaxy.apk
adb shell am start -n com.gathin.tvhome/.MainActivity
```

Atualizar por cima exige assinatura compatível. Guarde o APK anterior para recuperação e confira o botão Home físico após atualizar. A atualização não exige desinstalar a launcher nem alterar serviços do sistema.

## Créditos

CleanYourTV por Gathin e colaboradores, licença MIT: `LICENSE`. Fonte Outfit sob SIL Open Font License: `launcher/FONT-LICENSE-OFL.txt`. Galaxy e BorderGlow são adaptações nativas inspiradas nas referências do [React Bits](https://reactbits.dev/), sem executar React no Android. Outros créditos e licenças permanecem nos arquivos correspondentes.

Personalização conferida no MXQ Pro usado no desenvolvimento. Outros aparelhos ou firmwares podem apresentar comportamento diferente.
