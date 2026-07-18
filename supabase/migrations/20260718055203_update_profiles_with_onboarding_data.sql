ALTER TABLE profiles
ADD COLUMN IF NOT EXISTS dietary_preferences TEXT[] DEFAULT '{}',
ADD COLUMN IF NOT EXISTS category_preferences TEXT[] DEFAULT '{}',
ADD COLUMN IF NOT EXISTS primary_goal TEXT;