# Wildfire providers specifics

Episodes from CalFire and NIFC feeds may arrive frequently with minor updates.
To reduce noise the episode combinators for these providers create a new episode
only when something important changes. Two consecutive observations form one
episode if their name, severity, location and geometry remain the same.
