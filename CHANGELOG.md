# Changelog

## [1.0.6](https://github.com/mehmettguzell/STOP/compare/v1.0.5...v1.0.6) (2026-08-05)


### Bug Fixes

* add websocket port to docker compose ([ba4bd8d](https://github.com/mehmettguzell/STOP/commit/ba4bd8deaf37ee25106d8964fa3d6a0f2f70da69))

## [1.0.5](https://github.com/mehmettguzell/STOP/compare/v1.0.4...v1.0.5) (2026-08-05)


### Bug Fixes

* build and deploy actions ([6cbd088](https://github.com/mehmettguzell/STOP/commit/6cbd088f7791c3f4e779bf7972412eeb027fd05b))
* deploy via ssm job ([fe88e7e](https://github.com/mehmettguzell/STOP/commit/fe88e7ec6f05839b06292394c8d07caac41c610e))
* deploy via ssm job ([0cdce1f](https://github.com/mehmettguzell/STOP/commit/0cdce1f5b12c6b32b6dd5868ca1e685a0ea6e220))
* docker time problem ([b8bd080](https://github.com/mehmettguzell/STOP/commit/b8bd080ad236218cae871fe7cfb96a2070a5d0f8))
* docker-compose and push containers to ghcr ([2301d83](https://github.com/mehmettguzell/STOP/commit/2301d836267cc26f4add2cd11a961dba05ac1d75))
* test actions ([bdc19d4](https://github.com/mehmettguzell/STOP/commit/bdc19d43e64031b2bd150e2bb06952bde266b378))
* test actions ([daf6ff0](https://github.com/mehmettguzell/STOP/commit/daf6ff0788ae2dc46d335a9741d7497cea68caa7))
* workflow actions ([1411611](https://github.com/mehmettguzell/STOP/commit/1411611827b210ba55472197a8e3aee506c0e2cd))

## [1.0.4](https://github.com/mehmettguzell/STOP/compare/v1.0.3...v1.0.4) (2026-08-05)


### Bug Fixes

* **ci:** grant contents:write to notify-deploy for repository_dispatch ([6305f99](https://github.com/mehmettguzell/STOP/commit/6305f99212617d812cd093871671edbc8b7c2ad5))
* **ci:** pass gateway route env vars to the api-gateway smoke test, guard against overlapping builds ([c292626](https://github.com/mehmettguzell/STOP/commit/c292626a6a4a1957da73364afc90aa21be49ccf5))
* **ci:** trigger deploy via repository_dispatch, fix api-gateway env-file crash ([d6bf44d](https://github.com/mehmettguzell/STOP/commit/d6bf44d30e1a98aca920e6855eb4daf128406c7f))
* fix dockerfile ([bf7b833](https://github.com/mehmettguzell/STOP/commit/bf7b833ec2c874efbcea987f8086979ca7af7ab6))

## [1.0.3](https://github.com/mehmettguzell/STOP/compare/v1.0.2...v1.0.3) (2026-08-04)


### Bug Fixes

* **ci:** deploy latest tag, not commit SHA, and force first GHCR build ([6792d05](https://github.com/mehmettguzell/STOP/commit/6792d05c845f9a33356038d1481a51be79bca6cf))

## [1.0.2](https://github.com/mehmettguzell/STOP/compare/v1.0.1...v1.0.2) (2026-08-04)


### Bug Fixes

* **ci:** generate docker/.env from Secrets Manager before deploying ([a69c07e](https://github.com/mehmettguzell/STOP/commit/a69c07e42b6a3c9c3d0d91ae19a77bba5387286c))

## [1.0.1](https://github.com/mehmettguzell/STOP/compare/v1.0.0...v1.0.1) (2026-08-04)


### Performance Improvements

* **dev:** run mvn spring-boot:run offline in dev containers ([8615e2b](https://github.com/mehmettguzell/STOP/commit/8615e2bcbc7a0610ab6937cf54039f724a09bf3d))


### Reverts

* remove Testcontainers integration tests after repeated CI failures ([18a237a](https://github.com/mehmettguzell/STOP/commit/18a237a50b9bea3185977e07b510554246e2b527))

## 1.0.0 (2026-08-04)


### Features

* **ci:** build images in CI and deploy via SSM instead of SSH ([f321811](https://github.com/mehmettguzell/STOP/commit/f32181140facc6270f7ce5bde2331ae27d10b26a))


### Bug Fixes

* **ci:** add spring-boot-starter-test for @DataJpaTest/@AutoConfigureTestDatabase ([3ae3474](https://github.com/mehmettguzell/STOP/commit/3ae347450923a3cebb5bdfc4d77d72cfbe7389b4))
* **ci:** import testcontainers-bom instead of guessing a shared version ([cd8ee31](https://github.com/mehmettguzell/STOP/commit/cd8ee3106f1b0d3f1d1408c46e3f20ecbec41648))
* **ci:** pin explicit Testcontainers artifact versions ([11d116d](https://github.com/mehmettguzell/STOP/commit/11d116df29add9bf5923e850dd4b0e72b378e00b))
* **ci:** pin testcontainers.version to the last fully-published 1.x release ([c22bfe1](https://github.com/mehmettguzell/STOP/commit/c22bfe161f4e99a540c3879e28138d03f5adebd7))
