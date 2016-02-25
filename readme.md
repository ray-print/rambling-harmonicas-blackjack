#### House edge and strategy calculator for standard blackjack (probabilistic analysis)

## Background
This is the first program of any length I wrote; I stopped development in August 2014 and restarted in Feb 2016. The code needs serious refactoring (for several reasons -- I did it on a lark, not expecting to publish it; I was inexperienced when I wrote it; and I purposely used no external libraries.). It's in Java so it will obviously never compare to other calculators in terms of speed.

There is nothing new in this calculator in terms of algorithms. It uses a cache to speed up calculation of dealer hand results and approximates split hand results.

## Setup

Run:

```shell
 ./gradlew installDist
```

The executable should then be built in build/install/Blackjack/bin

## Command Line Examples

Get help:

```shell
./blackjack -?
```

Solves for the house edge and gives accurate strategy advice for a given rule set (assumes a console width of ~100 characters):

```shell
./blackjack --decks=4 --hsa=false --double-after-split=false --surrender=early --no-hole-card
(...description of rule set...)
                                          HARD HANDS                                        
                                         Dealer Up Card                                         
Player Cards           2       3       4       5       6       7       8       9      10       A
2, 3                   H       H       H       H       H       H       H       H       H       R
                       S       S       S       S       S       S       S       S       R       H
2, 4                   H       H       H       H       H       H       H       H       H       R
                       S       S       S       S       S       S       S       S       R       H
2, 5                   H       H       H       H       H       H       H       H       H       R
                       S       S       S       S       D       S       S       S       R       H
3, 4                   H       H       H       H       H       H       H       H       H       R
                       S       S       S       S       D       S       S       S       R       H
2, 6                   H       H       H       H       H       H       H       H       H       H
                       D       D       D       D       D       D       D       S       R       R
3, 5                   H       H       H       H       H       H       H       H       H       H
                       D       D       D       D       D       D       D       S       R       R
2, 7                   H       D       D       D       D       H       H       H       H       H
                       D       H       H       H       H       D       D       D       R       R
3, 6                   H       D       D       D       D       H       H       H       H       H
                       D       H       H       H       H       D       D       D       R       R
4, 5                   H       D       D       D       D       H       H       H       H       H
                       D       H       H       H       H       D       D       D       R       R
2, 8                   D       D       D       D       D       D       D       D       H       H
                       H       H       H       H       H       H       H       H       D       D
3, 7                   D       D       D       D       D       D       D       D       H       H
                       H       H       H       H       H       H       H       H       D       D
4, 6                   D       D       D       D       D       D       D       D       H       H
                       H       H       H       H       H       H       H       H       D       D
2, 9                   D       D       D       D       D       D       D       D       D       D
                       H       H       H       H       H       H       H       H       H       H
3, 8                   D       D       D       D       D       D       D       D       D       D
                       H       H       H       H       H       H       H       H       H       H
4, 7                   D       D       D       D       D       D       D       D       D       D
                       H       H       H       H       H       H       H       H       H       H
5, 6                   D       D       D       D       D       D       D       D       D       D
                       H       H       H       H       H       H       H       H       H       H
2, 10                  H       H       S       S       S       H       H       H       H       R
                       S       S       H       H       H       S       S       S       R       H
3, 9                   H       H       S       S       S       H       H       H       H       R
                       S       S       H       H       H       S       S       S       R       H
4, 8                   H       H       S       S       S       H       H       H       H       R
                       S       S       H       H       H       S       S       S       R       H
5, 7                   H       H       S       S       S       H       H       H       H       R
                       S       S       H       H       H       S       S       S       R       H
3, 10                  S       S       S       S       S       H       H       H       H       R
                       H       H       H       H       H       S       S       S       R       H
4, 9                   S       S       S       S       S       H       H       H       H       R
                       H       H       H       H       H       S       S       S       R       H
5, 8                   S       S       S       S       S       H       H       H       H       R
                       H       H       H       H       H       S       S       S       R       H
6, 7                   S       S       S       S       S       H       H       H       H       R
                       H       H       H       H       H       S       S       S       R       H
4, 10                  S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
5, 9                   S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
6, 8                   S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
5, 10                  S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
6, 9                   S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
7, 8                   S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
6, 10                  S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
7, 9                   S       S       S       S       S       H       H       H       R       R
                       H       H       H       H       H       S       S       S       H       H
7, 10                  S       S       S       S       S       S       S       S       S       R
                       H       H       H       H       H       H       H       H       R       S
8, 9                   S       S       S       S       S       S       S       S       S       R
                       H       H       H       H       H       H       H       H       R       S
8, 10                  S       S       S       S       S       S       S       S       S       S
                       H       H       H       H       H       H       H       H       R       R
9, 10                  S       S       S       S       S       S       S       S       S       S
                       H       H       H       H       H       H       H       H       R       R

                                          SOFT HANDS                                        
                                         Dealer Up Card                                         
Player Cards           2       3       4       5       6       7       8       9      10       A
A, 2                   H       H       H       D       D       H       H       H       H       H
                       D       D       D       H       H       D       D       D       R       R
A, 3                   H       H       H       D       D       H       H       H       H       H
                       D       D       D       H       H       D       D       D       R       R
A, 4                   H       H       D       D       D       H       H       H       H       H
                       D       D       H       H       H       D       D       D       R       R
A, 5                   H       H       D       D       D       H       H       H       H       H
                       D       D       H       H       H       D       D       D       R       R
A, 6                   H       D       D       D       D       H       H       H       H       H
                       D       H       H       H       H       D       D       D       S       R
A, 7                   D       D       D       D       D       S       S       H       H       H
                       S       S       S       S       S       D       H       S       S       S
A, 8                   S       S       S       S       D       S       S       S       S       S
                       D       D       D       D       S       D       D       H       H       H
A, 9                   S       S       S       S       S       S       S       S       S       S
                       D       D       D       D       D       D       D       D       H       H
A, 10                  S       S       S       S       S       S       S       S       S       S
                       S       S       S       S       S       S       S       S       S       S

                                         SPLIT HANDS                                        
                                         Dealer Up Card                                         
Player Cards           2       3       4       5       6       7       8       9      10       A
2, 2                   H       H       Y       Y       Y       Y       H       H       H       R
                       Y       Y       H       H       H       H       Y       Y       R       H
3, 3                   H       H       Y       Y       Y       Y       H       H       H       R
                       Y       Y       H       H       H       H       Y       Y       R       H
4, 4                   H       H       H       H       H       H       H       H       H       H
                       D       D       D       D       D       D       Y       Y       R       R
5, 5                   D       D       D       D       D       D       D       D       H       H
                       H       H       H       H       H       H       H       H       D       D
6, 6                   H       Y       Y       Y       Y       H       H       H       H       R
                       Y       H       S       S       S       Y       Y       S       R       H
7, 7                   Y       Y       Y       Y       Y       Y       H       H       R       R
                       S       S       S       S       S       H       Y       S       H       H
8, 8                   Y       Y       Y       Y       Y       Y       Y       Y       R       R
                       S       S       S       S       S       H       H       H       Y       Y
9, 9                   Y       Y       Y       Y       Y       S       Y       Y       S       S
                       S       S       S       S       S       Y       S       S       Y       Y
10, 10                 S       S       S       S       S       S       S       S       S       S
                       Y       Y       Y       Y       Y       Y       Y       Y       Y       Y
A, A                   Y       Y       Y       Y       Y       Y       Y       Y       Y       Y
                       H       H       H       H       D       H       H       H       H       H


The house edge is -0.0590% 


```

The program store strategy data in files; if the file is not found for a given rule set, it will calculate the data fresh based on a given rule set. 

The API can also be used to simply play through a blackjack game, but it does not contain the idea of chips or amount wagered or any graphics capabilities.

## Status
Results of this calculator matched those of other blackjack calculators, apart from split results (which tend to vary from calculator to calculator). If you find any discrepancy between the results here and those of other calculators, please let me know which results don't match. (Note this project is in alpha because of the shoddy state of testing and project organization, so if you are in need of reliable results you should go to the tried-and-true calculators.)
The project's API is usable, but the code needs clean-up.

Supported rule sets include:
* 1-8 decks
* Variable blackjack payback amount
* Early surrender / late surrender
* No hole card
* Resplit allowed (Multiple resplits not supported)
* Resplit aces (0-1 times)
* Hit split aces
* Dealer hit on soft 17
* Double on: 9-11, 10-11, any two cards, any 2-card hard total, any
number of cards, on split aces

Solved rule sets can be stored on disk so that calculations do not need to be repeated.
A standard rule set may take 5-10 seconds to calculate. If the dealer probability cache is used, then the first rule set will take much longer but then subsequent rule sets will be faster.

## Project Goals
* Use testing framework, refactor tests to match best practices, check code coverage, add test cases where applicable
* Set up tests to run on every commit using some CI tool
* Refactor program to follow best practices
* Change code architecture where appropriate, refactor overly long classes/functions
* Use third-party libraries where appropriate
* Remove dead/duplicate code
* Expose more functionality through the command line(different BJ payback amounts, EV for given hands, etc.)
* Avoid breaking API changes if possible

#### Long-term goals
* Create web front-end, make this available as an API 
* Store results in database to make data more accessible

## Contributions
Yes! They are welcome! Please let me know what you want to work on. The most important thing to do now is to refactor the tests: migrate them to JUnit, remove useless ones, and add unit tests where appropriate. Once that is done, then there are scores of TODOs -- just grab one and ping me and refactor away. Until the tests are done and obvious refactoring is completed, no new features should be worked on, but bug fixes and suggestions for API improvements are always welcome. Also please see the issues list.

## Acknowledgements
Thanks to all those who have solved this problem before, especially those who posted results online: John A. Nairn, the Wizard of Odds, Casino Vérité Software, bjstrat.net. I hope that this calculator will one day be as good as the already existing ones.
