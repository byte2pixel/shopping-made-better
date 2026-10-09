-- Grant UPDATE permission to the user who created the recipe
CREATE POLICY "Enable update for users based on created_by"
ON public.meals
FOR UPDATE
TO authenticated
USING (auth.uid() = created_by);

-- Grant DELETE permission to the user who created the recipe
CREATE POLICY "Enable delete for users based on created_by"
ON public.meals
FOR DELETE
TO authenticated
USING (auth.uid() = created_by);