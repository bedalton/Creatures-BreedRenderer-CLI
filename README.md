# Render Breed CLI tool

This is a Command line tool to render a preview of a norn

## Do Not Glob
Glob/Wildcards simply do not work in Windows, and neither do `./` prefixed paths 

**Best not to glob at all**

Avoid globbing the images directory especially with `./Images/*` or even `./images/[a-q][0-7][0-7][a-zA-Z].c16`.  
In C3DS these two patterns can be thousands of files. It is faster to simply use `./Images/`.
Using a less specific glob will slow down the CLI app to a crawl, 
taking several magnitudes longer, max out your CPUs and basically lead to a bad experience.  
If you must Glob, be as specific as possible, i.e.`./images/[a-q][04][0-7]m.c16`. 
Note when globbing a path for some females, it is sometimes required to include 
the male genus as well for breeds where the females use the male ATTs


### Usage
`render-creature {output} [...options] {sources}`

**Arguments**  
- `{output}` \[0\] -> Output PNG file (first argument)
  - If multiple poses are defined, this will be the prefix for generated files
- `{sources}` \[1..n\] -> Source folders and files    

**Options**
- Required
  - `--age` [0-6] see [Creature options](#creature-options)
  - `--gender` [m or f] see [Creature options](#creature-options)
  - `--breed` \[`--head`, `--body`, etc\] - see [Part Breeds](#part-breeds)
- [Part Breeds](#part-breeds) - default, head, body...
- [Pose](#pose) - Pose(s) to render
- [Creature Options](#creature-options) - age, gender, genome...
- [Color Options](#color-options) - tint, swap, rotate...
- [Render Options](#render-options) - scale, padding, trim whitespace...
- [Visibility Options](#part-visibility-options) - hide parts, semi-transparent parts...

**Type `render-creature --help` for arguments and options**

`render-creature`
### Options  

#### Part breeds

Format: **{genus}:{breed}**
    - **{genus}** - [n]orn, [g]rendel, [e]ttin, [s]hee, geat *Geat is not shortened to `G`(that's grendel), but `s`*
    - "norn:[a-zA-Z0-9]"
**Options**  
- `--breed` - The default breed to use for all parts not explicitly defined
- `--head`, `-h` -> **Head** breed
- `--body`, `-b` -> **Body** breed
- `--arms`, `-a` -> **Arms** breed
- `--legs`, `-l` -> **Legs** breed
- `--tail`, `-t` -> **Tail** Breed
- `--hair` -> **Hair** breed

#### Creature Options
- `--age`\[required\] -  The **age** of the creature to render 
- `--gender`\[required\] - **Gender** of creature to render; 1=Male, 2=Female
- `--genome` -> **Genetics file** or C2 egg file path
- `--gene-variant` \[default=0\] -> C2e genome variant, used when determining parts and colors
- `--export` -> **Creature export** file path
- `--mood`, `-m` -> The **Mood** to render the creature in 
  - \[All Games\]
    - `normal`
    - `happy`
    - `sad`
    - `angry`
  - \[C2e Only\]
    - `scared`
    - `sleepy`
  - \[CV Only\]
    - `very_sleepy_cv_only`
    - `very_happy_cv_only`
    - `mischievous_cv_only`
    - `very_scared_cv_only`
    - `ill_cv_only`
    - `hungry_cv_only`
- `--closed`, `-c` (flag) -> **Eyes closed**
- `--no-intersect` (flag) -> Use non-C2e limb z-order to prevent limb parts rendering both above and below another
#### Render Options
- `--game` - Game to render for. Value of [C1,C2,C3]
- `--scale` - **Scale** to use when rendering. Can be 1-10(inclusive)
  - Example: `--scale 2` = Scale image to 2x resolution
- `--trim` (flag) -> **Trim** whitespace around fully rendered image before adding padding
- `--padding`, `-d` -> Image **padding** around image. 
  - When not used with `--trim`, surrounding whitespace can be more than expected
- `--increment`, `-i` \[flag\] -> **Increment** numbers in filenames when files with names already exist
  - Without `--increment` existing files will be overwritten
- `--exact-match`, `-x` \[flag\] -> **Match sprite files exactly** (no substituting age or gender) or fail
  - Example `--exact-match`  - (Flag = without argument)
- `--open` \[flag\] -> Attempt to open rendered image in default image program
#### Part Visibility Options
- `--hidden` -> Parts to render as semi-transparent;
  - Example: `--hidden a,b,c,d` - commas, and no spaces
  - Example: `--hidden abcd` - no commas or spaces
- `--ghost` -> Parts to render as semi-transparent;
  - Example: `--ghost a,b,c,d` - commas, and no spaces
  - Example: `--ghost abcd` - no commas or spaces
- `--ghost-alpha` -> The alpha to use for ghost parts. 0.0..1.0 with 0 being completely transparent
  - Example `--ghost-alph 0.5`
- `--ghost-parts-below` (flag) -> Render ghost parts below solid ones
  - Example: `--ghost-parts-below` - (Flag = without argument)
#### Color Options
Color Values are 0-255; With 128 = No Change
- `--transform-variant` -> Use an alternate game's tint, swap and rotate methods; \[C1, C2, CV, C3, DS, SM\]; 
  - Example `--transform-variant C1`
- `--tint` -> Format `{red}:{green}:{blue}`, `#RRGGBB` (Hex color string)
  - Example: `--tint 10:255:13` or `--tint `
- `--swap -> Color swap between red and blue. Value: 0-255, 128 = No Change
  - Example `--swap 200`
- `--rotation` -> Color rotation shifting; Amount to shift 
  - rotation > 128 Red->Green->Blue(->Red)
  - rotation < 128 Blue->Green->Red(->Blue)
  - Example: `--rotation 90`

#### Pose
**Option**
- `--pose`, `-p` -> The creature **pose** to render
  **Format: 15 characters
- **Tilt** - `0-3`
  - `0` - down/back (furthest)
  - `1` - down/back (minor), or straight
  - `2` - forward/up (minor)
  - `3` - forward/up (furthest)
- char\[**0**\] - **Direction** -
  - `0` - Backwards
  - `1` - Forward
  - `2` - Right
  - `3` - Left;
  - Not used:
    - `?` - Towards "\_IT\_"
    - `!` - Away From `_IT_`
    - `X` - No Change
- char\[**1**\] - **Head** - { Tilt } `0-3`, `4` - Forward, `5` - Backwards
  - Not used:
    - `?`:Towards `_IT_`
    - `X`:No Change)
- char\[**2**\] - **Body** - { Tilt }
- char\[**3**\] - **Left Thigh** - { Tilt }
- char\[**4**\] - **Left Shin** - { Tilt }
- char\[**5**\] - **Left Foot** - { Tilt }
- char\[**6**\] - **Right Thigh** - { Tilt }
- char\[**7**\] - **Right Shin** - { Tilt }
- char\[**8**\] - **Right Foot** - { Tilt }
- char\[**9**\] - **Left Humerus** - { Tilt }
- char\[**10**\] - **Left Radius** - { Tilt }
- char\[**11**\] - **Right Humerus** - { Tilt }
- char\[**12**\] - **Right Radius** - { Tilt }
- char\[**13**\] - **Tail Root** - { Tilt }
- char\[**14**\] - **Tail Tip** - { Tilt }

**Auto Options**  
`--pose rand` - Generate a random pose, **Most likely weird**  
`--pose left` - Left facing default standing pose  
`--pose right` - Right facing default standing pose  
`--pose front` - Front facing default standing pose  
`--pose back` - Back facing default standing pose

**Multiple Poses**
You can define multiple poses to have them rendered using the same breed information.  
This is faster than running the command multiple times changing only the pose

If doing multiple images, the name of each rendered image will be the output file name,
and the pose number.

You can choose specific filenames for pose by separating the pose
from the name using `:` (colon)  
**Example**:`--pose 312222222111112:"Facing Left"` would create an image called `Facing Left.png`,
for the pose given

**Pose Examples**  
Standing facing left - `312222222111112`
- char\[**0**\] - Direction: **3** Left
- char\[**1**\] - Head - **1** Straight
- char\[**2**\] - Body - **2** - Up/Forward
- char\[**3**\] - Left Thigh - **2** - Up/Forward
- char\[**4**\] - Left Shin - **2** - Up/Forward
- char\[**5**\] - Left Foot - **2** - Up/Forward
- char\[**6**\] - Right Thigh - **2** - Up/Forward
- char\[**7**\] - Right Shin - **2** - Up/Forward
- char\[**8**\] - Right Foot - **2** - Up/Forward
- char\[**9**\] - Left Humerus - **1** - Back (minor)
- char\[**10**\] - Left Radius - **1** - Back (minor)
- char\[**11**\] - Right Humerus - **1** - Back (minor)
- char\[**12**\] - Right Radius - **1** - Back (minor)
- char\[**13**\] - Tail Base - **2** - Up/Forward
- char\[**14**\] - Tail Tip - **2** - Up/Forward

#### Other
- `--debug` - Add additional debug logging
- `--verbose` \[flag\] -> Use verbose logging
- `--help` -> Print usage info
- `--open` -> Open image after generation
- `--print-alter-genome-command` \[flag\] -> Print an equivalent alter genome statement minus the filename for the breed-util
- `--loop` \[flag\] -> Loop random generations, until use writes `no` or `n`


### Normal Render

Render a typical ron norn  

Command: 
```BASH
render-breed.exe C1 ron-norn-m.png--age 3 --gender m --breed n:6 --scale 2 --pose 312222222111110 "./Images" "./Body Data"
```

--------

## Appendix: Command Line Basics

### Input Files: Paths

Paths can be relative to the [current working directory](#current-working-directory) or absolute.  
On windows paths are separated with `\\`. When viewing examples, keep this in mind

Any time a file or folder is needed, you may drag one into the command line window

### Current Working Directory

**Current working directory** is the path your terminal or Command Prompt thinks you are.

- Usually when starting a command prompt or terminal, this is your **HOME** directory
- **This is not usually the folder you have open** or are viewing when you open the command prompt.
- To Navigate to the folder you want, us `cd ` (plus a space)
  - then enter/paste the absolute path to the folder
    - `c:/MyFolder` or `"/Users/{myname}/My Folder"`
  - **drag** the folder from explore,finder, etc. into the CMD or Terminal window
  - If typing in a folder with a space, surround it with quotation marks

### Arguments

Arguments are values that are defined by their position.  
They do not use a prefix they are simply used.   
In this README, they will be surrounded by `{` and `}`  
i.e. `breed-util convert-breed {game-target}`

- `breed-util convert-breed` is entered as is,
- `{game-target}` must be substituted by the target game
- Example: `breed-util convert-breed C2`

### Options

come after the `breed-util` command and its subcommand, and come in two flavors

#### Regular Options

Most options are followed by a value i.e. `--input-genus norn`.

- Here the *option* is `--input-genus` and the *value* is `norn`

##### Flags

Flags take **no arguments**, and represent a **true** value to the flag

- Flags take no argument so do not put `yes` or `no` after it
- **Using a flag** = **true**; Presence of a flag, means that value is true or enabled
- **No flag** = **false**; Absence of flag means that value is false

---

## Appendix: macOS

### Open Terminal

You can open terminal through [spotlight](#open-terminal-from-spotlight) or [Finder](#open-terminal-through-finder)

#### Open Terminal From Spotlight

1. Open Spotlight
  - Shortcut Key - `CMND+SPACE`
  - OR Click the magnifying-glass icon on the menubar in the upper right
2. Type in `Terminal`
3. Select `Terminal.app` and hit enter
4. \[Optional] Set your current working directory if desired using `cd` (see [current working directory](#current-working-directory))

#### Open Terminal Through Finder

1. Open Finder
2. Open the `Applications` Folder
3. Open the `Utilities` folder
4. Double Click `Terminal.app`
5. \[Optional] Set your current working directory if desired using `cd` (see [current working directory](#current-working-directory))

### GATEKEEPER

MacOS may prevent you from opening the executable as the binary is not signed

#### Allow Program in Terminal

1. copy and paste `xattr -d com.apple.quarantine `
2. Add a space after it
3. Drag the `breed-util` executable into the folder and hit enter
4. Enter password if prompted

#### Allow Program in System Setting

1. Open System Settings
2. On the left side find `Privacy and Security`
3. Find `Security`
4. Click `Open Anyways`
5. Enter Password if prompted

## Appendix: Windows
### Open CMD (command line window)
1. Click the search bar next to the windows icon
2. Type `CMD`
3. Click `Command Prompt`
4. \[Optional] Set your current working directory if desired using `cd` (see [current working directory](#current-working-directory)) 

