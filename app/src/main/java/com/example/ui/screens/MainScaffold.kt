package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PharaohGold
import com.example.ui.theme.PharaohGoldContainer
import com.example.ui.theme.PharaohGoldDark
import com.example.ui.theme.PharaohGoldLight
import com.example.ui.theme.PharaohNavy
import com.example.ui.theme.PharaohNavyDark
import com.example.ui.theme.PharaohNavyLight
import com.example.ui.viewmodel.PharaohsViewModel
import com.example.ui.viewmodel.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    viewModel: PharaohsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.currentRole == UserRole.GUEST) {
        AuthScreen(uiState = uiState, viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_pharaohs_logo),
                                contentDescription = "شعار الفراعنة",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الفراعنة",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PharaohGoldLight
                                )
                                Text(
                                    text = when (uiState.currentRole) {
                                        UserRole.ADMIN -> "بوابة مسؤول النظام والتحكم (Admin)"
                                        UserRole.DOCTOR -> "د. ${uiState.currentDoctorProfile?.name ?: "هيئة التدريس"} • ${uiState.currentDoctorProfile?.departmentName ?: ""}"
                                        UserRole.STUDENT -> uiState.currentStudent?.fullName ?: "طالب"
                                        else -> "زائر"
                                    },
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        // Role badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (uiState.currentRole) {
                                UserRole.ADMIN -> PharaohGold
                                UserRole.DOCTOR -> PharaohGoldContainer
                                else -> PharaohNavyLight
                            }
                        ) {
                            Text(
                                text = when (uiState.currentRole) {
                                    UserRole.ADMIN -> "👑 مسؤول النظام"
                                    UserRole.DOCTOR -> "👨‍🏫 هيئة التدريس"
                                    UserRole.STUDENT -> "🎓 طالب"
                                    else -> "زائر"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (uiState.currentRole) {
                                    UserRole.ADMIN, UserRole.DOCTOR -> PharaohNavyDark
                                    else -> Color.White
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("btn_logout")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "تسجيل الخروج",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PharaohNavyDark,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = PharaohNavyDark,
                    contentColor = PharaohGoldLight
                ) {
                    if (uiState.currentRole == UserRole.ADMIN) {
                        NavigationBarItem(
                            selected = uiState.activeNavTab == 0,
                            onClick = { viewModel.setActiveNavTab(0) },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            label = { Text("لوحة التحكم", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_admin_control")
                        )

                        NavigationBarItem(
                            selected = uiState.activeNavTab == 1,
                            onClick = { viewModel.setActiveNavTab(1) },
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            label = { Text("المحتوى والمقررات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_academic")
                        )

                        NavigationBarItem(
                            selected = uiState.activeNavTab == 2,
                            onClick = { viewModel.setActiveNavTab(2) },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            label = { Text("جدول الامتحانات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_exams")
                        )

                        NavigationBarItem(
                            selected = uiState.activeNavTab == 3,
                            onClick = { viewModel.setActiveNavTab(3) },
                            icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            label = { Text("الكارنيهات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_card")
                        )
                    } else if (uiState.currentRole == UserRole.DOCTOR) {
                        NavigationBarItem(
                            selected = uiState.activeNavTab == 0,
                            onClick = { viewModel.setActiveNavTab(0) },
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            label = { Text("المحاضرات والمقررات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_academic")
                        )

                        NavigationBarItem(
                            selected = uiState.activeNavTab == 1,
                            onClick = { viewModel.setActiveNavTab(1) },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            label = { Text("جدول الامتحانات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_exams")
                        )

                        NavigationBarItem(
                            selected = uiState.activeNavTab == 2,
                            onClick = { viewModel.setActiveNavTab(2) },
                            icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                            label = { Text("تسليمات الطلاب", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_submissions")
                        )
                    } else {
                        // Student navigation
                        NavigationBarItem(
                            selected = uiState.activeNavTab == 0,
                            onClick = { viewModel.setActiveNavTab(0) },
                            icon = { Icon(Icons.Default.School, contentDescription = null) },
                            label = { Text("المحتوى الأكاديمي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_academic")
                        )

                        NavigationBarItem(
                            selected = uiState.activeNavTab == 1,
                            onClick = { viewModel.setActiveNavTab(1) },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            label = { Text("جدول الامتحانات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_exams")
                        )

                         NavigationBarItem(
                            selected = uiState.activeNavTab == 2,
                            onClick = { viewModel.setActiveNavTab(2) },
                            icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            label = { Text("الكارنيه الجامعي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PharaohNavyDark,
                                selectedTextColor = PharaohGoldLight,
                                indicatorColor = PharaohGold,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            ),
                            modifier = Modifier.testTag("nav_tab_card")
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = PharaohNavyDark,
                        contentColor = PharaohGoldLight
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.currentRole == UserRole.ADMIN) {
                    when (uiState.activeNavTab) {
                        0 -> AdminDashboardScreen(uiState = uiState, viewModel = viewModel)
                        1 -> AcademicContentScreen(uiState = uiState, viewModel = viewModel)
                        2 -> ExamScheduleScreen(uiState = uiState, viewModel = viewModel)
                        3 -> StudentCardScreen(uiState = uiState, viewModel = viewModel)
                    }
                } else if (uiState.currentRole == UserRole.DOCTOR) {
                    when (uiState.activeNavTab) {
                        0 -> AcademicContentScreen(uiState = uiState, viewModel = viewModel)
                        1 -> ExamScheduleScreen(uiState = uiState, viewModel = viewModel)
                        2 -> DoctorSubmissionsScreen(uiState = uiState, viewModel = viewModel)
                    }
                } else {
                    when (uiState.activeNavTab) {
                        0 -> AcademicContentScreen(uiState = uiState, viewModel = viewModel)
                        1 -> ExamScheduleScreen(uiState = uiState, viewModel = viewModel)
                        2 -> StudentCardScreen(uiState = uiState, viewModel = viewModel)
                        else -> AcademicContentScreen(uiState = uiState, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
