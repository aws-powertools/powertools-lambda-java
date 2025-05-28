---
title: Maintainers playbook
description: Process
---

<!--  markdownlint-disable MD043 -->

## Overview

!!! note "Please treat this content as a living document."

This is document explains who the maintainers are, their responsibilities, and how they should be doing it. If you're interested in contributing,
 see [CONTRIBUTING](https://github.com/aws-powertools/powertools-lambda-java/blob/main/CONTRIBUTING.md){target="_blank"}.

## Current Maintainers

| Maintainer      | GitHub ID                                                            | Affiliation |
| --------------- | -------------------------------------------------------------------- | ----------- |
| Philipp Page    | [phipag](https://github.com/phipag){target="\_blank" rel="nofollow"} | Amazon      |
| Simon Thulbourn | [sthulb](https://github.com/sthulb){target="\_blank" rel="nofollow"} | Amazon      |

## Emeritus

Previous active maintainers who contributed to this project.

| Maintainer            | GitHub ID                                                                              | Affiliation   |
| --------------------- | -------------------------------------------------------------------------------------- | ------------- |
| Jerome Van Der Linden | [jeromevdl](https://github.com/jeromevdl){target="\_blank" rel="nofollow"}             | Amazon        |
| Michele Ricciardi     | [mriccia](https://github.com/mriccia){target="\_blank" rel="nofollow"}                 | Amazon        |
| Scott Gerring         | [scottgerring](https://github.com/scottgerring){target="\_blank" rel="nofollow"}       | DataDog       |
| Mark Sailes           | [msailes](https://github.com/msailes){target="\_blank" rel="nofollow"}                 | Former Amazon |
| Pankaj Agrawal        | [pankajagrawal16](https://github.com/pankajagrawal16){target="\_blank" rel="nofollow"} | Former Amazon |
| Steve Houel           | [stevehouel](https://github.com/stevehouel){target="\_blank" rel="nofollow"}           | Amazon        |

## Labels

These are the most common labels used by maintainers to triage issues, pull requests (PR), and for project management:

| Label                            | Usage                                                                                             | Notes                                              |
|----------------------------------|---------------------------------------------------------------------------------------------------|----------------------------------------------------|
| triage                           | New issues that require maintainers review                                                        | Issue template                                     |
| bug                              | Unexpected, reproducible and unintended software behavior                                         | PR/Release automation; Doc snippets are excluded;  |
| documentation                    | Documentation improvements                                                                        | PR/Release automation; Doc additions, fixes, etc.; |
| duplicate                        | Dupe of another issue                                                                             |                                                    |
| enhancement                      | New or enhancements to existing features                                                          | Issue template                                     |
| RFC                              | Technical design documents related to a feature request                                           | Issue template                                     |
| help wanted                      | Tasks you want help from anyone to move forward                                                   | Bandwidth, complex topics, etc.                    |
| feature-parity                   | Adding features present in other Powertools for Lambda libraries                                  |                                                    |
| good first issue                 | Somewhere for new contributors to start                                                           |                                                    |
| governance                       | Issues related to project governance - contributor guides, automation, etc.                       |                                                    |
| question                         | Issues that are raised to ask questions                                                           |                                                    |
| maven                            | Related to the build system                                                                       |                                                    |
| need-more-information            | Missing information before making any calls                                                       |                                                    |
| status/staged-next-release       | Changes are merged and will be available once the next release is made.                           |                                                    |
| status/staged-next-major-release | Contains breaking changes - merged changes will be available once the next major release is made. |                                                    |
| blocked                          | Issues or PRs that are blocked for varying reasons                                                | Timeline is uncertain                              |
| priority:1                       | Critical - needs urgent attention                                                                 |                                                    |
| priority:2                       | High - core feature, or affects 60%+ of users                                                     |                                                    |
| priority:3                       | Neutral - not a core feature, or affects < 40% of users                                           |                                                    |
| priority:4                       | Low - nice to have                                                                                |                                                    |
| priority:5                       | Low - idea for later                                                                              |                                                    |
| invalid                          | This doesn't seem right                                                                           |                                                    |
| size/XS                          | PRs between 0-9 LOC                                                                               | PR automation                                      |
| size/S                           | PRs between 10-29 LOC                                                                             | PR automation                                      |
| size/M                           | PRs between 30-99 LOC                                                                             | PR automation                                      |
| size/L                           | PRs between 100-499 LOC                                                                           | PR automation                                      |
| size/XL                          | PRs between 500-999 LOC, often PRs that grown with feedback                                       | PR automation                                      |
| size/XXL                         | PRs with 1K+ LOC, largely documentation related                                                   | PR automation                                      |
| dependencies                     | Changes that touch dependencies, e.g. Dependabot, etc.                                            | PR/ automation                                     |
| maintenance                      | Address outstanding tech debt                                                                     |                                                    |

## Maintainer Responsibilities

Maintainers are active and visible members of the community, and have 
[maintain-level permissions on a repository](https://docs.github.com/en/organizations/managing-access-to-your-organizations-repositories/repository-permission-levels-for-an-organization){target="_blank"}. 
Use those privileges to serve the community and evolve code as follows.

Be aware of recurring ambiguous situations and [document them](#common-scenarios) to help your fellow maintainers.

### Uphold Code of Conduct

<!-- markdownlint-disable-next-line MD013 -->
Model the behavior set forward by the
[Code of Conduct](https://github.com/aws-powertools/powertools-lambda-java/blob/main/CODE_OF_CONDUCT.md){target="_blank"}
and raise any violations to other maintainers and admins. There could be unusual circumstances where inappropriate 
behavior does not immediately fall within the [Code of Conduct](https://github.com/aws-powertools/powertools-lambda-java/blob/main/CODE_OF_CONDUCT.md){target="_blank"}.

These might be nuanced and should be handled with extra care - when in doubt, do not engage and reach out to other maintainers 
and admins.

### Prioritize Security

Security is your number one priority. Maintainer's Github keys must be password protected securely and any reported 
security vulnerabilities are addressed before features or bugs.

Note that this repository is monitored and supported 24/7 by Amazon Security, see
[Security disclosures](https://github.com/aws-powertools/powertools-lambda-java/){target="_blank"} for details.

### Review Pull Requests

Review pull requests regularly, comment, suggest, reject, merge and close. Accept only high quality pull-requests. 
Provide code reviews and guidance on incoming pull requests.

PRs are [labeled](#labels) based on file changes and semantic title. Pay attention to whether labels reflect the current
state of the PR and correct accordingly.

Use and enforce [semantic versioning](https://semver.org/){target="_blank" rel="nofollow"} pull request titles, as these will be used for
[CHANGELOG](https://github.com/aws-powertools/powertools-lambda-java/blob/main/CHANGELOG.md){target="_blank"}
and [Release notes](https://github.com/aws-powertools/powertools-lambda-java/releases) - make sure they communicate their 
intent at the human level.

For issues linked to a PR, make sure `status/staged-next-release` label is applied to them when merging.
[Upon release](#releasing-a-new-version), these issues will be notified which release version contains their change.

See [Common scenarios](#common-scenarios) section for additional guidance.

### Triage New Issues

Manage [labels](#labels), review issues regularly, and create new labels as needed by the project. Remove `triage` 
label when you're able to confirm the validity of a request, a bug can be reproduced, etc. 
Give priority to the original author for implementation, unless it is a sensitive task that is best handled by maintainers.

Make sure issues are assigned to our [board of activities](https://github.com/orgs/aws-powertools/projects/4).

Use our [labels](#labels) to signal good first issues to new community members, and to set expectation that this might 
need additional feedback from the author, other customers, experienced community members and/or maintainers.

Be aware of [casual contributors](https://opensource.com/article/17/10/managing-casual-contributors){target="_blank" rel="nofollow"} and recurring contributors. 
Provide the experience and attention you wish you had if you were starting in open source.

See [Common scenarios](#common-scenarios) section for additional guidance.

### Triage Bug Reports

Be familiar with [our definition of bug](#is-that-a-bug). If it's not a bug, you can close it or adjust its title and 
labels - always communicate the reason accordingly.

For bugs caused by upstream dependencies, replace `bug` with `bug-upstream` label. Ask the author whether they'd like to
raise the issue upstream or if they prefer us to do so.

Assess the impact and make the call on whether we need an emergency release. Contact other [maintainers](#current-maintainers) when in doubt.

See [Common scenarios](#common-scenarios) section for additional guidance.

### Triage RFCs

RFC is a collaborative process to help us get to the most optimal solution given the context. Their purpose is to ensure
everyone understands what this context is, their trade-offs, and alternative solutions that were part of the research 
before implementation begins.

Make sure you ask these questions in mind when reviewing:

- Does it use our [RFC template](https://github.com/aws-powertools/powertools-lambda-java/issues/new?assignees=&labels=RFC%2C+triage&projects=&template=rfc.md&title=RFC%3A+)?
- Does it match our [Tenets](https://docs.powertools.aws.dev/lambda/java/latest/#tenets)?
- Does the proposal address the use case? If so, is the recommended usage explicit?
- Does it focus on the mechanics to solve the use case over fine-grained implementation details?
- Can anyone familiar with the code base implement it?
- If approved, are they interested in contributing? Do they need any guidance?
- Does this significantly increase the overall project maintenance? Do we have the skills to maintain it?
- If we can't take this use case, are there alternative projects we could recommend? Or does it call for a new project altogether?

When necessary, be upfront that the time to review, approve, and implement a RFC can vary - 
see [Contribution is stuck](#contribution-is-stuck). Some RFCs may be further updated after implementation, as certain areas become clearer.

Some examples using our initial and new RFC templates: #92, #94, #95, #991, #1226

### Releasing a new version

!!! note "The release process is currently a long, multi-step process. The team is in the process of automating at it."

Firstly, make sure the commit history in the `main` branch **(1)** it's up to date, **(2)** commit messages are semantic, 
and **(3)** commit messages have their respective area, for example `feat: <change>`, `chore: ...`).

**Looks good, what's next?**

Kickoff the `Prepare for maven central release` workflow with the intended rekease version. Once this has completed, it will
draft a Pull Request named something like `chore: Prep release 1.19.0`. the PR will **(1)** roll all of the POM versions
forward to the new release version and **(2)** release notes. 

Once this is done, check out the branch and clean up the release notes. These will be used both in the
[CHANGELOG.md file](https://github.com/aws-powertools/powertools-lambda-java/blob/main/CHANGELOG.md)
file and the [published github release information](https://github.com/aws-powertools/powertools-lambda-java/releases),
and you can use the existing release notes to see how changes are summarized.

Next, commit and push, wait for the build to complete, and merge to main. Once main has built successfully (i.e. build, tests and end-to-end tests should pass), create a 
tagged release from the Github UI, using the same release notes.

Next, run the `Publish package to the Maven Central Repository` action to release the library.

Finally, by hand, create a PR rolling all of the POMs onto the next snapshot version (e.g. `1.20.0-SNAPSHOT`). 


### Add Continuous Integration Checks

Add integration checks that validate pull requests and pushes to ease the burden on Pull Request reviewers. 
Continuously revisit areas of improvement to reduce operational burden in all parties involved.

### Negative Impact on the Project
<!-- markdownlint-disable-next-line MD013 -->
Actions that negatively impact the project will be handled by the admins, in coordination with other maintainers, 
in balance with the urgency of the issue. Examples would be
[Code of Conduct](https://github.com/aws-powertools/powertools-lambda-java/blob/main/CODE_OF_CONDUCT.md){target="_blank"}
violations, deliberate harmful or malicious actions, spam, monopolization, and security risks.

## Common scenarios

These are recurring ambiguous situations that new and existing maintainers may encounter. They serve as guidance. 
It is up to each maintainer to follow, adjust, or handle in a different manner as long as 
[our conduct is consistent](#uphold-code-of-conduct)

### Contribution is stuck

A contribution can get stuck often due to lack of bandwidth and language barrier. For bandwidth issues, 
check whether the author needs help. Make sure you get their permission before pushing code into their existing PR -
do not create a new PR unless strictly necessary.

For language barrier and others, offer a 1:1 chat to get them unblocked. Often times, English might not be their 
primary language, and writing in public might put them off, or come across not the way they intended to be.

In other cases, you may have constrained capacity. Use `help wanted` label when you want to signal other maintainers 
and external contributors that you could use a hand to move it forward.

### Insufficient feedback or information

When in doubt, use the `need-more-information` label to signal more context and feedback are necessary before proceeding. 

### Crediting contributions

We credit all contributions as part of each [release note](https://github.com/aws-powertools/powertools-lambda-java/releases){target="_blank"}
as an automated process. If you find  contributors are missing from the release note you're producing, please add them manually.

### Is that a bug?

A bug produces incorrect or unexpected results at runtime that differ from its intended behavior. 
Bugs must be reproducible. They directly affect customers experience at runtime despite following its recommended usage.

Documentation snippets, use of internal components, or unadvertised functionalities are not considered bugs.

### Mentoring contributions

Always favor mentoring issue authors to contribute, unless they're not interested or the implementation is sensitive (_e.g., complexity, time to release, etc._).

Make use of `help wanted` and `good first issue` to signal additional contributions the community can help.

### Long running issues or PRs

Try offering a 1:1 call in the attempt to get to a mutual understanding and clarify areas that maintainers could help.

In the rare cases where both parties don't have the bandwidth or expertise to continue, it's best to use the `revisit-in-3-months` label. By then, see if it's possible to break the PR or issue in smaller chunks, and eventually close if there is no progress.
