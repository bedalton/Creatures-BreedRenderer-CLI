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


## Usage

### CLI Basics

#### Absolute vs Relative paths
When using


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

#### Other
- `--debug` - Add additional debug logging     
- `--verbose` \[flag\] -> Use verbose logging
- `--help` -> Print usage info


### Normal Render

Render a typical ron norn  

Command: ```
render-breed.exe C1 ron-norn-m.png--age 3 --gender m --breed n:6 --scale 2 --pose 312222222111110 ./Images ./Body^ Data
```
