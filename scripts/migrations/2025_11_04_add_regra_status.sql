-- Migration: Add status column to regra table to support validation workflow
-- Created: 2025-11-04

-- Safe check: add column only if it does not exist
-- Note: Some MySQL versions don't support IF NOT EXISTS for ADD COLUMN. Adjust if needed.
ALTER TABLE `regra`
  ADD COLUMN `status` VARCHAR(20) NULL DEFAULT 'EM_VALIDACAO';

-- Optional: Backfill existing rows to EM_VALIDACAO to ensure consistency
UPDATE `regra` SET `status` = COALESCE(`status`, 'EM_VALIDACAO');

-- Verification query (manual):
-- SELECT COUNT(*) FROM `regra` WHERE `status` IS NULL;