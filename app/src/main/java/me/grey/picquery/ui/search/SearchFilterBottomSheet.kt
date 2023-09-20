package me.grey.picquery.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.grey.picquery.ui.albums.AlbumViewModel
import me.grey.picquery.ui.albums.EmptyAlbumTips

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchFilterBottomSheet(
    searchViewModel: SearchViewModel = viewModel(),
    albumViewModel: AlbumViewModel = viewModel()
) {
    val open by rememberSaveable { searchViewModel.isFilterOpen }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { searchViewModel.closeFilterBottomSheet(sheetState) } },
            sheetState = sheetState,
        ) {
            val list = remember { albumViewModel.searchableAlbumList }
            if (list.isEmpty()) {
                EmptyAlbumTips(
                    onClose = {
                        scope.launch { albumViewModel.closeBottomSheet(sheetState) }
                    },
                )
            } else {
                Box(modifier = Modifier.padding(vertical = 20.dp)) {
                    SearchAbleAlbums()
                }
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun SearchAbleAlbums(
    albumViewModel: AlbumViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    val searchRange = remember { searchViewModel.searchRange }
    val all = remember { albumViewModel.searchableAlbumList }
    FlowRow(
        Modifier.padding(horizontal = 12.dp)
    ) {
        repeat(all.size) { index ->
            val album = all[index]
            val selected = searchRange.contains(album)

            val colors = FilterChipDefaults.elevatedFilterChipColors(
                iconColor = MaterialTheme.colorScheme.primary,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            )

            ElevatedFilterChip(
                modifier = Modifier.padding(horizontal = 6.dp),
                colors = colors,
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = if (selected) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.AddCircleOutline
                        },
                        contentDescription = "",
                    )
                },

                selected = selected,
                onClick = { searchViewModel.toggleToRange(album) },
                label = { Text(text = "${album.label} (${album.count})") }
            )
        }
    }
}