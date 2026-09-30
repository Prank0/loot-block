# Loot Block

A RuneLite plugin that provides:

- a searchable item catalogue for adding blocked items;
- a searchable list for reviewing and removing blocked items;
- automatic deprioritization of blocked items in ground-loot stacks;
- prevention of pickup and targeted ground-item actions for blocked items;
- the chat message `Looting this item is disabled.` when a blocked action is attempted.

Noted and unnoted variants are stored under the same canonical item ID.

## Development

Build and test with Java 11:

```text
./gradlew clean test
./gradlew run
```

The `run` task starts RuneLite in developer mode with this plugin loaded.
