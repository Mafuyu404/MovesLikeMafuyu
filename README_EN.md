# Moves Like Mafuyu

This is a lightweight movement mod designed to improve exploration and building movement.

### Sliding

- Press sneak while sprinting to start a slide. The player gains a small boost and stays low enough to pass through one-block-high spaces.

- After the slide duration ends, the player resumes sprinting. Sliding can be canceled by jumping or moving backward. Canceling with backward movement will not restore sprinting.

- When a slide leaves the ground, its duration is refreshed, allowing sustained sliding over sloped terrain.

- When sliding onto water, the player bounces upward slightly and keeps sliding forward. This action is called "water skipping" and has a default limit of two skips.

- Extra: while sliding, colliding with an entity directly in front of the player's view will knock that entity away, and the sliding player will lose momentum.

### Climbing

- When facing a two-block-high obstacle, jumping allows the player to climb it in the same style as vanilla climbing. Mechanically, it behaves as if invisible ladders were placed around the upper block.

- Climbing will not work if the block above the obstacle is not a solid-render block, such as slabs, leaves, or ice; if the player is not facing the obstacle; or if the obstacle is only one block high.

- While stopping on a climbable surface by holding sneak, pressing jump performs a normal jump. This is useful for three-block-high obstacles whose top block is not solid-rendered.

- If the player slips off a block edge, facing the original block and holding sneak can stop the fall. This works because the climb check is more forgiving while falling.

### Swimming

- Press sprint while touching water to enter swimming, even when just entering water or standing in one-block-deep water.

- Swimming straight upward to the surface lets the player breathe and keep swimming at the same time. This is called "freestyle swimming". It already exists in vanilla, but this mod makes it much easier to trigger.

- While swimming, pressing sprint gives the player a short forward boost and consumes some air. This is called "swimming boost".

- While freestyle swimming, pressing jump launches the player out of the water in the view direction and starts a slide followed by water skipping. This is called "water glide".

### Crawling

- Double tap sneak to enter crawling. This is equivalent to vanilla movement in a one-block-high space. Press sneak once to cancel it.

- By default, the player can still jump while crawling. A config option can make the jump key cancel crawling instead.

- Shortly after a sprint jump, pressing sneak enters crawling and grants a small boost. This is called "leap". Landing does not automatically cancel crawling, and leap can clear 1.5-block-high obstacles.

- While crawling, pressing sneak starts a slide. The slide ending does not automatically cancel crawling.

### Rolling

- Hold a movement key and double tap sprint to roll. If already sprinting, pressing sprint once triggers it by default, though a config option can require a double tap. Rolling can also be triggered in the air.

- Rolling on the ground grants continuous directional speed and can pass through 1.5-block-high spaces. Rolling in the air normally grants no immediate boost, but it will gain speed after landing.

- Shortly after a sprint jump, pressing sprint can trigger an aerial boosted roll. This is called "air flip" and can clear 1.5-block-high obstacles.

- Rolling grants invulnerability frames by default, which can be used to dodge attacks or cushion fall landings.

### Commands

- Use `/moveslikecommand attribute` to set movement attribute overrides for one or more players. Overrides take priority over the config file, and clearing an override makes the player use the config value again.

- Example: `/moveslikecommand attribute @p slideStartBoost set 1.2` increases one player's slide start speed, while `/moveslikecommand attribute @p slideStartBoost clear` clears that override.

- Overridable attributes include all numeric attribute config entries for sliding, climb jump, swimming boost, leap, crawling, rolling, air flip, and related actions.

- KubeJS can also modify the player's Capability directly. On 1.21 and above, this should be implemented through data attachments.

### Other

- Sneak, jump, sprint. That's it, now you know how to use the mod. Although it has many features, none of them require extra key bindings.

- Recommended for use with Smart Key Prompts, which can intelligently show available actions.

- Most features can be adjusted in the config file, and server config takes priority. For example, you can configure whether sprinting slide requires a double tap, whether sliding ignores sneak edge protection, which entities can be knocked away by sliding, whether rolling rotates the first-person view, whether aerial rolling is allowed, and whether rolling grants invulnerability frames. Numeric attributes can also be configured, such as slide acceleration, roll animation speed, leap window duration, water skipping limit, and slide cooldown.

- When used together with TaCZ, crawling conflicts because both mods simulate vanilla crawling with different detection logic. The TaCZ addon handles compatibility by disabling TaCZ crawling and allowing players to shoot while sliding.

- With more complex key sequences, these simple actions can produce harder movement tech. For example, sprint slide, instantly jump, chain into air flip, then cancel into leap and sprint until landing. This is estimated to cover eight blocks, and you can still climb a one-block-high obstacle afterward. More features may be added later to raise the skill ceiling without affecting normal use.

- The Chinese name of this mod comes from a joke in the Bilibili comment section.
