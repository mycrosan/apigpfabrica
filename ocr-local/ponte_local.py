"""Expõe o OCR ao host sem conceder saída de rede ao contêiner do motor."""
import asyncio
import logging


async def copiar(origem, destino):
    while dados := await origem.read(65536):
        destino.write(dados)
        await destino.drain()


async def encaminhar(leitor, escritor):
    destino = None
    tarefas = []
    try:
        remoto, destino = await asyncio.wait_for(
            asyncio.open_connection('ocr', 8091), timeout=5)
        # O destino é fixo: esta ponte não aceita endereços fornecidos pelo cliente.
        tarefas = [asyncio.create_task(copiar(leitor, destino)),
                   asyncio.create_task(copiar(remoto, escritor))]
        concluidas, _ = await asyncio.wait(
            tarefas, timeout=65, return_when=asyncio.FIRST_COMPLETED)
        for tarefa in concluidas:
            tarefa.result()
    except (OSError, asyncio.TimeoutError) as erro:
        logging.warning('Falha na conexão local do OCR: %s', type(erro).__name__)
    finally:
        for tarefa in tarefas:
            tarefa.cancel()
        await asyncio.gather(*tarefas, return_exceptions=True)
        escritor.close()
        if destino is not None:
            destino.close()


async def iniciar():
    servidor = await asyncio.start_server(encaminhar, '0.0.0.0', 8091)
    async with servidor:
        await servidor.serve_forever()


if __name__ == '__main__':
    asyncio.run(iniciar())
