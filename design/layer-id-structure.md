# Layer ID Structure

The API returns map layers that are referenced by their IDs.  Most layers have a
constant ID because they do not depend on any context.  Event shapes are an
exception – their geometry depends on the event they belong to.  Using the same
ID for all event shapes caused accidental mismatches when associating
information with a layer.

To avoid this problem each context‑dependent layer includes its context ID in
the layer ID.  For event shapes the ID is built as `eventShape::<eventId>`.

Example for event `56e8c85a-10e6-44f5-9cf0-51c6016a3e87`:

```
eventShape::56e8c85a-10e6-44f5-9cf0-51c6016a3e87
```

The `::` separator is chosen because it is unlikely to appear in an original
layer ID.  Code that needs only the base ID can split the string on this
separator and take the first part (`eventShape`).  Layers that are independent of
any context keep their original ID without a suffix.
