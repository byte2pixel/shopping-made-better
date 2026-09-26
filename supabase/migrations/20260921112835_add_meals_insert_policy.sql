-- Add a column to track who created the custom recipe
ALTER TABLE public.meals ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES auth.users(id) DEFAULT auth.uid();

-- Grant permission for authenticated users to insert their own meals
CREATE POLICY "Enable insert for authenticated users"
ON public.meals
FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = created_by);