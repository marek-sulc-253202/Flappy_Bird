from django.urls import path
from players import views

urlpatterns = [
    path('players/', views.list_players, name='list_players'),
    path('players/add/', views.add_player, name='add_player'),
    path('players/update_score/', views.update_score, name='update_score'),
    path('players/delete/', views.delete_player, name='delete_player'),
]
