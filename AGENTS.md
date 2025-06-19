# Notes for agents

Documentation:
 - Check out README.md. 
 - Check endpoints and database schema as described in `docs/`.
 - docs/ folder has general project documentation that needs to be kept up to date.
 - Fix everything in the `docs/` folder to match reality.
 - Don't update `README.md` with minor code fixes.
 - Every feature needs to have comprehensive up-to-date documentation near it.
 - API documentation is using Swagger, its descriptions should be clear for data consumers who don't have access to codebase.

Debugging:
 - Add enough debug logs so you can find out what's wrong but not be overwhelmed when something does not work as expected.
 - Code test coverage is measured by codecov. Write useful tests to increase it and check key requirements to hold.
 - When refactoring to move a feature, don't forget to remove the original code path.

Style:
 - Add empty lines between logical blocks as in the rest of the codebase.
 - Clean stuff up if you can: fix typos, make lexics more correct in English.
 - Write enough comments so you can deduce what was a requirement in the future.
 - Write insightful code comments.
 - Do not break indentation.
 - Do not mix tabs and spaces.
 - Format the code nicely and consistently.
 - Do not replace URLs with non-existing ones

Java:
 - Write enough comments so that people proficient in Python, PostGIS can grasp the Java code.
 - Just ignoring exceptions is not the best fix, handle in a better way

SQL:
 - prefer indexed operators when dealing with jsonb ( `tags @> '{"key": "value"}` instead of `tags ->> 'key' = 'value'` ).
 - SQL is lowercase, PostGIS functions follow their spelling from the manual (`st_segmentize` -> `ST_Segmentize`).
 - values in layers should be absolute as much as possible: store "birthday" or "construction date" instead of "age".
 - SQL files should to be idempotent: drop table if exists; add some comments to make people grasp quereies faster.
 - Format queries in a way so it's easy to copy them out of the codebase and debug standalone.
 - Do not rewrite old migrations, not for style changes, not for logic changes, always create new migrations for any changes in DB

Make:
 - Makefile: If you need intermediate result from other target, split it into two and depend on the intermediate result.
 - Makefile: there are comments on the same line after each target separated by ## - they are used in debug graph visualization, need to be concise and descriptive of what's going on in the code itself.
 - trivial oneliner SQLs are okay to keep in Makefile.

Pull requests:
 - Use Conventional Commits convention when formatting the pull request and commits, e.g. `type(scope): TICKETNUMBER title ...`. Skip ticket number if not provided. Field: Public Id.
 - Branch names should match branch name recorded by Fibery if provided (e.g. "21648-switch-page-after-login-to-map"). Field: Branch Name.
 - Reference Fibery task in the PR description. Field: Task URL.
 - Github Actions is used as CI. Update it as necessary.
