import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { useOrg } from '../auth/OrgProvider';

export function OrgSwitcher() {
  const { organizations, activeOrg, setActiveOrg } = useOrg();

  if (organizations.length === 0) {
    return null;
  }

  return (
    <FormControl size="small" sx={{ minWidth: 180 }}>
      <InputLabel id="org-switcher-label">Organization</InputLabel>
      <Select
        labelId="org-switcher-label"
        label="Organization"
        value={activeOrg?.id ?? ''}
        onChange={(e) => setActiveOrg(e.target.value)}
      >
        {organizations.map((org) => (
          <MenuItem key={org.id} value={org.id}>
            {org.name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
