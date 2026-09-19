## ADDED Requirements

### Requirement: Publicación de releases de Android

El repositorio SHALL publicar una GitHub Release al empujar un tag que empiece con `v`, con el APK de release adjunto, y el `versionName` del APK SHALL corresponder al tag publicado.

#### Scenario: Tag de versión dispara la release

- **WHEN** se empuja el tag `v0.1`
- **THEN** el workflow `release.yml` corre, compila el APK de release y crea la Release `v0.1` con el APK adjunto

#### Scenario: Versión coherente con el tag

- **WHEN** se instala el APK de la Release `v0.1`
- **THEN** el sistema reporta la versión `0.1` de la aplicación

#### Scenario: Tests antes de publicar

- **WHEN** el workflow de release se ejecuta
- **THEN** corre los tests unitarios de Android y no publica la Release si fallan

#### Scenario: APK instalable

- **WHEN** se descarga el APK adjunto a la Release y se instala en un dispositivo Android 8 o superior
- **THEN** la instalación finaliza correctamente y la app abre mostrando la pantalla inicial de FitLog

#### Scenario: Trazabilidad de firma

- **WHEN** el proyecto todavía no tiene keystore propio
- **THEN** la Release indica que el APK está firmado con la clave de depuración y que no es apto para tiendas
