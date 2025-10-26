-- Add new business logic fields to inventory_items table
ALTER TABLE inventory_items 
ADD COLUMN min_stock_level INTEGER,
ADD COLUMN max_stock_level INTEGER,
ADD COLUMN reorder_quantity INTEGER,
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN is_discontinued BOOLEAN NOT NULL DEFAULT false;

-- Add indexes for new fields
CREATE INDEX idx_inventory_items_is_active ON inventory_items(is_active);
CREATE INDEX idx_inventory_items_is_discontinued ON inventory_items(is_discontinued);
CREATE INDEX idx_inventory_items_min_stock_level ON inventory_items(min_stock_level);

-- Add comments for new fields
COMMENT ON COLUMN inventory_items.min_stock_level IS 'Minimum stock level before reorder alert';
COMMENT ON COLUMN inventory_items.max_stock_level IS 'Maximum stock level for this item';
COMMENT ON COLUMN inventory_items.reorder_quantity IS 'Quantity to reorder when below min_stock_level';
COMMENT ON COLUMN inventory_items.is_active IS 'Whether the item is active for reservations';
COMMENT ON COLUMN inventory_items.is_discontinued IS 'Whether the item is discontinued';

-- Update existing records to have proper default values
UPDATE inventory_items 
SET is_active = true, 
    is_discontinued = false 
WHERE is_active IS NULL OR is_discontinued IS NULL;
