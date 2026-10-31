# Comparison Tree: плагин IntelliJ IDEA для сравнения реализаций в JUnit

Тесты пишутся на обычном JUnit: реализации под сравнением задаются через `@ParameterizedClass`. Плагин показывает их в
окне результатов IntelliJ листьями под каждым тестом: `класс > метод > [набор] > реализация` вместо
`класс > реализация > метод > [набор]`. Переставляются только классы и тесты с отметкой `@ComparisonTree`.

| Без плагина | С плагином |
|---|---|
| `Good > mul > [1], [2]`<br>`Good > mulFourByFive`<br>`Buggy > mul > [1], [2] ✘`<br>`Buggy > mulFourByFive ✘` | `mul > [1] > Good, Buggy`<br>`mul > [2] > Good, Buggy ✘`<br>`mulFourByFive > Good, Buggy ✘` |

## Быстрый старт

Нужен JDK 21 в `JAVA_HOME`.

```
./gradlew publishToMavenLocal   # кладёт @ComparisonTree в ~/.m2, его использует example/
./gradlew runIde                # IDE-песочница с плагином
```

При первом запуске `runIde` скачивает IntelliJ IDEA Community 2025.2.6.3. Чтобы взять уже установленную IDE, добавьте
`-PideaHome="<путь к IDE>"` (или пропишите `ideaHome` в `~/.gradle/gradle.properties`).

В песочнице откройте папку `example/` как проект (Maven). У `MulTest` нажмите иконку ▶ → Run: тесты покажутся по
реализациям. В `MarkedTestTest` отмечен один тест, остальные остаются деревом Jupiter.

## Установка в свою IDE

```
./gradlew buildPlugin
```

Затем Settings | Plugins | ⚙ | Install Plugin from Disk… → `build/distributions/junit-comparison-plugin-0.1.0-SNAPSHOT.zip`.

## В своём проекте

1. Подключите аннотацию. После `./gradlew publishToMavenLocal` она лежит в `~/.m2`:

   ```xml
   <dependency>
       <groupId>org.atpfivt.comparison</groupId>
       <artifactId>comparison-annotations</artifactId>
       <version>0.1.0-SNAPSHOT</version>
       <scope>test</scope>
   </dependency>
   ```

   В Gradle: `testImplementation("org.atpfivt.comparison:comparison-annotations:0.1.0-SNAPSHOT")` и `mavenLocal()` в
   репозиториях. Можно и просто скопировать
   [`ComparisonTree.java`](comparison-annotations/src/main/java/org/atpfivt/comparison/ComparisonTree.java) в тесты:
   плагин ищет аннотацию по имени `org.atpfivt.comparison.ComparisonTree`. JUnit её не видит.
2. Задайте реализации через `@ParameterizedClass` и поставьте отметку:

   ```java
   @ParameterizedClass(name = "{argumentSetName}")
   @MethodSource("examples.Calc#implementations") // argumentSet("Good", new Good()), argumentSet("Buggy", new Buggy())
   @ComparisonTree
   class MulTest {

       @Parameter
       Calc calc;

       @Test
       void mulFourByFive() {
           assertEquals(20, calc.mul(4, 5));
       }
   }
   ```
3. Тесты должен запускать раннер IntelliJ. В Maven-проектах так по умолчанию. В Gradle-проектах выберите Settings |
   Build, Execution, Deployment | Build Tools | Gradle → Run tests using: **IntelliJ IDEA**.

## Отметка `@ComparisonTree`

- **На `@ParameterizedClass`** — его вызовы считаются реализациями для всех тестов класса, `@Nested` тоже. Вложенный
  `@ParameterizedClass` без своей отметки остаётся уровнем дерева: `Inner > factor 2 > mulByFive() > Good`.
- **На тесте или `@Nested`-классе** — переставляется только он, по вызовам ближайшего внешнего
  `@ParameterizedClass`. Остальные тесты остаются под `Good`/`Buggy`:

  ```
  MarkedTestTest
  ├─ Good
  │  └─ mulTwoByTwo() ✔
  ├─ mulFourByFive()
  │  ├─ Good ✔
  │  └─ Buggy ✘
  └─ Buggy
     └─ mulTwoByTwo() ✔
  ```
- Работает и как мета-аннотация, наследуется подклассами.
- Без отметок поток событий не меняется вовсе.

## Запуск: два способа

- **Обычный Run** (иконка ▶, контекстное меню, конфигурации JUnit). Включается переключателем **Run | Comparison Tree for
  JUnit Runs** (последний пункт меню Run), по умолчанию включён.
- **Отдельная конфигурация `JUnit (comparison tree)`**: пункт «Run '… (comparison tree)'» в конце меню иконки ▶ у
  классов и тестов, которых касается отметка. Работает и при выключенном переключателе.

## Как устроен

Раннер JUnit в IntelliJ печатает события (`##teamcity[testStarted nodeId='…' parentNodeId='…']`), где `nodeId` —
UniqueId JUnit. Плагин переписывает этот поток уже в IDE, по ходу прогона, а JVM тестов не трогает.

- `transpose/` — ядро без зависимостей от IDE. `TreeTransposer` сливает одинаковые узлы разных вызовов
  `[class-template-invocation:#k]` в узел с UniqueId без вызова, а тест делает листом с именем реализации и настоящим
  UniqueId. Поэтому перезапуск узла из дерева работает: Jupiter выполняет объединённый узел во всех реализациях.
- `annotation/AnnotatedMarks` — по именам класса и метода из UniqueId находит код и проверяет `@ComparisonTree`. Метод
  сравнивается по стёртым типам параметров, поэтому перегрузки различаются.
- `configuration/` — конфигурация `JUnit (comparison tree)`: наследник `JUnitConfiguration`, свой конвертер вывода через
  `SMCustomMessagesParsing`.
- `console/` — обычные JUnit-запуски: `com.intellij.java.consoleDecorator` оборачивает консоль и подменяет для неё поток
  процесса переставленным.

## Разработка

```
./gradlew test                                  # 30 тестов
IDEA_HOME="<путь к IDE>" ./capture-logs.sh      # перезаписать логи раннера, нужен Maven (mvn или MVN=...)
```

Тесты прогоняют записанные логи настоящего раннера IntelliJ 2025.2 (`src/test/resources/logs`, классы из `example/`)
через ядро и через классы самой IDE, из которых строится окно результатов. Отметки в них берутся из кода тестового
проекта. Отдельно проверены поиск отметок, меню иконки ▶ и регистрация плагина.

## Ограничения

- Меняется только дерево. Порядок выполнения остаётся порядком Jupiter: сначала все `Good`, потом все `Buggy`.
- Пары строятся по позиции в UniqueId, а не по значениям. Если источник аргументов у реализаций вернёт разные данные
  или `@TestFactory` создаст разные тесты, пары будут неверными (`fixtures.DynamicFixture`).
- Только раннер IntelliJ: при запуске через Gradle события приходят без UniqueId.
- Пока IDE индексирует проект, отметок не видно, и дерево остаётся деревом Jupiter.
- Конфигурация `JUnit (comparison tree)`: перезапуск узла из дерева результатов идёт через обычный Run. Если переключатель
  выключен, дерево будет исходным.
- Обычный Run подменяет консоль запуска обёрткой. Функции IDE, которые проверяют её тип (`SMTRunnerConsoleView`), могут
  не сработать.
