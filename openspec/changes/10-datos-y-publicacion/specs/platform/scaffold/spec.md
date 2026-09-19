## MODIFIED Requirements

### Requirement: Publicación de releases de Android

El repositorio SHALL publicar una GitHub Release al empujar un tag que empiece con `v`, con el APK de release adjunto firmado con el keystore propio del proyecto, y SHALL derivar `versionName` del tag y `versionCode` del historial de commits.

#### Scenario: Tag de versión dispara la release

- **WHEN** se empuja el tag `v0.1.7`
- **THEN** el workflow `release.yml` corre, compila el APK de release, lo firma con el keystore de los secretos y crea la Release con el APK adjunto

#### Scenario: Versión coherente con el tag

- **WHEN** se instala el APK de la Release `v0.1.7`
- **THEN** el sistema reporta la versión `0.1.7` de la aplicación

#### Scenario: Tests antes de publicar

- **WHEN** el workflow de release se ejecuta
- **THEN** corre los tests unitarios de Android y no publica la Release si fallan

#### Scenario: APK instalable

- **WHEN** se descarga el APK adjunto a la Release y se instala en un dispositivo Android 8 o superior
- **THEN** la instalación finaliza correctamente y la app abre mostrando la pantalla inicial de FitLog

#### Scenario: Firma con keystore propio

- **WHEN** el keystore existe en el runner porque el workflow decodificó `KEYSTORE_BASE64`
- **THEN** el APK de release se firma con esa clave y no con la clave de depuración

#### Scenario: Firma de depuración como respaldo

- **WHEN** el keystore no está disponible (por ejemplo en un build local)
- **THEN** el build de release no se firma con la clave de depuración y la aplicación avisa en el README que ese APK no es distribuible

#### Scenario: Trazabilidad de firma

- **WHEN** el APK se firma con el keystore propio
- **THEN** la Release indica la huella de la clave de firma para poder verificarla
