from django.http import JsonResponse, HttpResponse
from django.views.decorators.csrf import csrf_exempt
from .models import Player
import json

def list_players(request):
    players = list(Player.objects.values())
    return JsonResponse(players, safe=False)

@csrf_exempt
def add_player(request):
    if request.method == 'POST':
        data = json.loads(request.body)
        player, created = Player.objects.get_or_create(name=data['name'])
        return JsonResponse({'status': 'ok', 'id': player.id})
    return HttpResponse(status=405)

@csrf_exempt
def update_score(request):
    if request.method == 'POST':
        data = json.loads(request.body)
        player = Player.objects.get(name=data['name'])

        diff = data['difficulty']
        score = data['score']

        if diff == 0: player.score_easy = max(player.score_easy, score)
        elif diff == 1: player.score_normal = max(player.score_normal, score)
        elif diff == 2: player.score_hard = max(player.score_hard, score)

        player.save()
        return JsonResponse({'status': 'updated'})
    return HttpResponse(status=405)

@csrf_exempt
def delete_player(request):
    if request.method == 'POST':
        data = json.loads(request.body)
        Player.objects.filter(name=data['name']).delete()
        return JsonResponse({'status': 'deleted'})
    return HttpResponse(status=405)
