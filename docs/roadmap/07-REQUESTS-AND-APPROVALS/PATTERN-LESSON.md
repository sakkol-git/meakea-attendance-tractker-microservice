# Adapter pattern lesson

## Admission test

This is a conditional curriculum target, not a claim that current source justifies the pattern. Refactor only after all four answers below are yes; otherwise keep plain code and record rejection.

- [ ] A real current HRFlow requirement exists.
- [ ] The plain tested version already exists and hurts demonstrably.
- [ ] Two real variations or an isolation/composition need exists.
- [ ] A junior explains the change in two sentences.

## Requirement

Filesystem setup and failure injection spread across attachment tests.

## Baseline

First implement the plain method/constructor with real tests. Commit that version and save its output before reading the refactor. This is the first step of Stone 3.

## Pain

Show the duplicated or untestable lines in two real cases and explain one concrete maintenance mistake they invite. If no pain exists, do not invent it. This is the second step of Stone 3.

## Refactor

StoragePort isolates disk I/O; disk adapter and test adapter share behavior tests. Keep the same behavior/negative tests; compare diff and output with baseline. This is the third step of Stone 3. No critical transaction or authorization invariant moves.

## Class roles

- Caller: names intent and owns authorization/transaction boundary. It does not know provider details.
- Pattern helper: handles construction, adaptation or composition only. It never establishes trusted tenant identity.
- Test: asserts before/after results and the relevant failure/isolation case. It does not mirror implementation internals.

## Runnable example

Copy the complete example into `/tmp/PatternExample.java`. It is Java 21-compatible learning code, not production HR authorization or persistence.

```java
import java.util.HashMap;
import java.util.Map;
public class PatternExample {
    interface StoragePort { void put(String key, byte[] value); byte[] get(String key); }
    static class TestStorage implements StoragePort {
        private final Map<String, byte[]> values = new HashMap<>();
        public void put(String key, byte[] value) { values.put(key, value.clone()); }
        public byte[] get(String key) { return values.get(key).clone(); }
    }
    public static void main(String[] args) {
        StoragePort store = new TestStorage();
        byte[] input = {1, 2};
        store.put("tenant-a/file", input);
        input[0] = 9;
        if (store.get("tenant-a/file")[0] != 1) throw new AssertionError("copy");
        System.out.println("adapter checks passed");
    }
}
```

```bash
javac -d /tmp /tmp/PatternExample.java
java -cp /tmp PatternExample
```

## Unit tests

The example includes executable assertions in main. Production refactor also keeps the existing JUnit Jupiter (Boot-managed JUnit 6) domain/repository tests green. Add the following small JUnit file to the new app test source only during the lesson; do not introduce a test dependency in this documentation task.

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
class PatternExampleTest {
    @Test void exampleKeepsBehavior() {
        assertDoesNotThrow(() -> PatternExample.main(new String[0]));
    }
}
```

## Debugging

Compare unchanged inputs/results first. Inspect tenant value and immutable copies, then transaction boundary, then helper configuration. If a refactor breaks a test, restore the plain implementation on a new recovery branch and reduce the diff.

## Trade-offs

Extra types add navigation and maintenance cost. This lesson buys clearer intent/isolation only if the baseline pain is real. Do not optimize pattern count.

## When not to use

A single trivial file helper without test isolation pain can stay a method.

## Spot it later

Look for repeated optional construction, external I/O leakage, or repeated query composition respectively; ask for a failing maintenance example before proposing a named pattern.

## Pattern review

- [ ] Admission answers and before/after evidence are recorded.
- [ ] Same tests pass and junior explanation takes two sentences.
- [ ] Retro records whether it was worth it, or why a simple version won.

Status: **Not run**. [Primer](../primers/ADAPTER.md) and [learning path](../PATTERNS-LEARNING-PATH.md).
