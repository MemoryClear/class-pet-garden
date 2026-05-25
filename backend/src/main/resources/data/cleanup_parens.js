const fs = require('fs');
const d = JSON.parse(fs.readFileSync('./evolution_rules.json', 'utf8'));

let fixed = 0;
Object.values(d).flat().forEach(r => {
  const orig = r.condition || '';
  // Remove ALL parenthetical content (any character between any parenthesis-like chars)
  // Matches: (xxx) / （xxx） / （xxx / xxx）
  const cleaned = orig
    .replace(/\([^)]{0,200}\)/g, '')          // ASCII (xxx)
    .replace(/（[^\uff09]{0,200}\uff09/g, '')  // fullwidth （xxx）
    .replace(/\uff09[^\uff09]{0,200}/g, '');  // xxx） tail
  // Trim whitespace
  const trimmed = cleaned.replace(/\s+/g, ' ').trim();
  if (trimmed !== orig.trim()) {
    r.condition = trimmed;
    fixed++;
    console.log('  ' + orig + ' => ' + trimmed);
  }
});

console.log('total fixed:', fixed);

// Check remaining parens
const still = Object.values(d).flat().filter(r => {
  const c = r.condition || '';
  return c.includes('\uff08') || c.includes('\uff09');  // fullwidth ( )
});
console.log('still has fullwidth parens:', still.length);
if (still.length > 0) still.forEach(r => console.log(' ', r.condition));

fs.writeFileSync('./evolution_rules.json', JSON.stringify(d, null, 2), 'utf8');
console.log('Done.');
