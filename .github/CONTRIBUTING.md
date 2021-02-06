# :green_heart: How to contribute

Want to contribute to Looping Layout? Awesome! Thank you for your interest =)

These guidelines are meant to make it easier for you to contribute, and make it
easier for the maintainers to review your contribution. Following these guidelines
helps to communicate that you respect the time of maintainers, and makes
it easier for them to get back to *you* in a timely fashion.

There are many different ways to contribute to this project including: writing
blog posts, submitting bug reports or feature requests, contributing to the code,
or even just adding a Looping Layout to your project!

## :loudspeaker: Feedback

One of the biggest things you can do is give feedback about what features
are important to you, and why those features are important. This makes it easier
to prioritize what to work on, and gets you what you want faster!

So before you submit a new issue, see if someone has already requested
that feature or reported that bug, and try to comment on their post instead. This keeps
all similar requests together, so it's easy to tell what's popular.

## :tada: Code contribution 

Unsure where to start contributing? Well issues labeled [good-first-issue][good-first-issue]
are a great place to start. These issues should have a well-defined
use case or problem, and a plan of attack for how to tackle it. They're a good
way to get more familiar with the project.

[help-wanted][help-wanted] issues are also a good place to start, but they can be a
bit more in-depth. Often they solve a larger problem than issues labeled
[good-first-issue][good-first-issue] or they involve some creative thinking and design.

### :star2:Getting started

To submit your first code contribution follow the below steps.

1. Claim an issue.
1. Fork the repository.
2. Create a branch following the branch guidelines.
3. Fix the issue.
4. Test your changes!
5. Submit a pull request against develop.  
   Be sure to follow the template!
   
After that a maintainer will assign the pull request to their self, and
try to get back to you as soon as possible. Other people are free
to review pull requests, but the maintainers (e.g. BeksOmega) get final
say on what gets approved and merged.

Once all feedback on your pull request has been fixed/discussed a maintainer
will merge it into develop. And it will be released on master once the
maintainers feel there is enough content to create a release.

Congratulations! You did it!

### :hand: Before you code

Before starting work on an issue please write a comment on it "claiming" it.
Something as simple as "I'll take this one" works perfectly. Claiming
issues makes sure that people aren't doing duplicate work. If you get
busy, or decide the issue you claimed isn't the one for you, please
delete your comment claiming it.

If you have a different problem that doesn't have an issue associated with it
please create an issue for it. This allows people to discuss and respond
to the problem. Or better yet, someone else may have run into your problem
and has a solution that could work for you!

### :deciduous_tree: Branch guidelines

All contributions should branch from `develop` and merge into `develop`. 
This makes it easier to create releases and push to Maven Central.

There are no specific naming conventions for branches, and there are no
particular commit message rules.

Just try to make your branches as reviewer-friendly as possible. This means:
1. Keep your changes small. Fix one issue at a time.
2. Split changes into multiple commits.

   If you can say "I did X, then Y, then Z." Those should probably be
   three commits. Something as simple as separating your changes from
   your tests can be a big help!

Just a few simple things like that make it faster and easier to respond
to your pull request.

## :memo: Issue submitting

Submitting issues is one of the most important ways to contribute to this
project! Issues make it easier to address problems, get feedback, and
prioritize tasks.

### :bug: Bug reporting

To submit a bug report just follow the pre-created issue [template][issue-template]. Be sure
to fill out all of the sections! Even if you don't think they're affecting
your problem.

Also be sure to check that there aren't duplicate issues, including closed ones!
Minimizing duplicate issues helps keep things organized and makes it easier
to prioritize.

### :sparkles: Feature requesting

The Looping Layout project's goal is to create a robust LayoutManager to
work with the Android RecyclerView.

Currently the project is focused on creating a linear version (i.e. akin
to the LinearLayoutManager) but if you have a use for a grid/staggered
looping layout manager be sure to tell us!

To submit a feature request just follow the pre-created [template][issue-template].

Be sure to check that your feature request hasn't already been submitted by
someone else. If there is already an existing issue feel free to comment
on it and add your opinion. Participation is always appreciated!

### :checkered_flag: After submission

After your issue has been submitted a maintainer will respond to you as soon
as possible. They may ask you for more information, add it to the [good-first-issue][good-first-issue]
or [help-wanted][help-wanted] lists, or they may assign it to someone. The exact
procedure is very dependent on the impact and context of the issue.

## :heart: Thank you!

Thank you again for your interest in contributing to this project. Contributions
are always appreciated, no matter what form they come in =)

[good-first-issue]: https://github.com/BeksOmega/looping-layout/issues?q=is%3Aissue+is%3Aopen+label%3Agood-first-issue
[help-wanted]: https://github.com/BeksOmega/looping-layout/issues?q=is%3Aissue+is%3Aopen+label%3help-wanted
[issue-template]: https://github.com/BeksOmega/looping-layout/issues/new/choose
[semver]: https://semver.org/
[milestone]: https://github.com/BeksOmega/looping-layout/milestone/1
